package ma.aui.csc.monolith.healthlink.maintenance.service;

import java.util.Objects;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.audit.data.entity.AuditAction;
import ma.aui.csc.monolith.healthlink.audit.service.AuditService;
import ma.aui.csc.monolith.healthlink.common.data.*;
import ma.aui.csc.monolith.healthlink.common.error.WorkflowConflictException;
import ma.aui.csc.monolith.healthlink.common.security.StaffAccess;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.*;
import ma.aui.csc.monolith.healthlink.equipment.data.repository.EquipmentRepository;
import ma.aui.csc.monolith.healthlink.equipment.notification.EquipmentEvents;
import ma.aui.csc.monolith.healthlink.maintenance.data.WorkOrderMapper;
import ma.aui.csc.monolith.healthlink.maintenance.data.entity.*;
import ma.aui.csc.monolith.healthlink.maintenance.data.repository.WorkOrderRepository;
import ma.aui.csc.monolith.healthlink.staff.data.entity.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import static ma.aui.csc.monolith.healthlink.maintenance.data.WorkOrderDtos.*;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkOrderService {
    private final WorkOrderRepository repository;
    private final EquipmentRepository equipmentRepository;
    private final StaffAccess staffAccess;
    private final WorkOrderMapper mapper;
    private final AuditService audit;
    private final EquipmentEvents events;

    @Transactional
    @PreAuthorize("hasRole('RADIOLOGY_OPERATOR')")
    public WorkOrderResponse reportFault(@NotNull @Valid ReportFaultRequest request) {
        Equipment equipment = equipmentRepository.findById(request.equipmentId())
                .filter(Equipment::isActive).orElseThrow(() -> notFound("Equipment"));
        StaffMember actor = staffAccess.requireDepartmentOperator(equipment.getDepartment());
        requireVersion(request.equipmentVersion(), equipment.getVersion());
        if (repository.existsByOpenEquipmentId(equipment.getId())) {
            throw new WorkflowConflictException("This equipment already has an open work order");
        }
        if (equipment.getStatus() == EquipmentStatus.UNDER_MAINTENANCE) {
            throw new WorkflowConflictException("Equipment already under maintenance cannot start another repair");
        }
        EquipmentStatus previousStatus = equipment.getStatus();
        WorkOrder order = repository.save(new WorkOrder(equipment, actor, request.description()));
        equipment.changeStatus(EquipmentStatus.OUT_OF_SERVICE);
        return finish(order, actor, AuditAction.FAULT_REPORTED, previousStatus, null, request.description());
    }

    @Transactional
    @PreAuthorize("hasRole('MAINTENANCE_TECHNICIAN')")
    public WorkOrderResponse accept(Long id, @NotNull @Valid VersionRequest request) {
        WorkOrder order = requiredOrder(id);
        StaffMember actor = staffAccess.requireHospitalRole(hospitalId(order), StaffRole.MAINTENANCE_TECHNICIAN);
        requireMutableEquipment(order);
        requireVersion(request.version(), order.getVersion());
        requireEquipmentStatus(order, EquipmentStatus.OUT_OF_SERVICE);
        String previousOrderStatus = order.getStatus().name();
        order.accept(actor);
        order.getEquipment().changeStatus(EquipmentStatus.UNDER_MAINTENANCE);
        return finish(order, actor, AuditAction.REPAIR_ACCEPTED, EquipmentStatus.OUT_OF_SERVICE,
                previousOrderStatus, "Repair accepted by technician");
    }

    @Transactional
    @PreAuthorize("hasRole('MAINTENANCE_TECHNICIAN')")
    public WorkOrderResponse complete(Long id, @NotNull @Valid CompleteRepairRequest request) {
        WorkOrder order = requiredOrder(id);
        StaffMember actor = staffAccess.requireHospitalRole(hospitalId(order), StaffRole.MAINTENANCE_TECHNICIAN);
        requireMutableEquipment(order);
        requireVersion(request.version(), order.getVersion());
        requireEquipmentStatus(order, EquipmentStatus.UNDER_MAINTENANCE);
        String previousOrderStatus = order.getStatus().name();
        order.complete(actor, request.repairNotes());
        return finish(order, actor, AuditAction.REPAIR_COMPLETED, EquipmentStatus.UNDER_MAINTENANCE,
                previousOrderStatus, request.repairNotes());
    }

    @Transactional
    @PreAuthorize("hasRole('HOSPITAL_MANAGER')")
    public WorkOrderResponse verify(Long id, @NotNull @Valid ReviewRepairRequest request) {
        return review(id, request, true);
    }

    @Transactional
    @PreAuthorize("hasRole('HOSPITAL_MANAGER')")
    public WorkOrderResponse reject(Long id, @NotNull @Valid ReviewRepairRequest request) {
        return review(id, request, false);
    }

    private WorkOrderResponse review(Long id, ReviewRepairRequest request, boolean approved) {
        WorkOrder order = requiredOrder(id);
        StaffMember actor = staffAccess.requireHospitalManager(hospitalId(order));
        requireMutableEquipment(order);
        requireVersion(request.version(), order.getVersion());
        requireEquipmentStatus(order, EquipmentStatus.UNDER_MAINTENANCE);
        String previousOrderStatus = order.getStatus().name();
        if (approved) {
            order.verify(actor, request.notes());
            order.getEquipment().changeStatus(EquipmentStatus.AVAILABLE);
        } else {
            order.reject(actor, request.notes());
        }
        return finish(order, actor, approved ? AuditAction.REPAIR_VERIFIED : AuditAction.REPAIR_REJECTED,
                EquipmentStatus.UNDER_MAINTENANCE, previousOrderStatus, request.notes());
    }

    @PreAuthorize("hasAnyRole('HOSPITAL_MANAGER', 'RADIOLOGY_OPERATOR', 'MAINTENANCE_TECHNICIAN')")
    public WorkOrderResponse get(Long id) {
        WorkOrder order = requiredOrder(id);
        staffAccess.requireEquipmentAccess(order.getEquipment());
        return mapper.toResponse(order);
    }

    @PreAuthorize("hasAnyRole('HOSPITAL_MANAGER', 'RADIOLOGY_OPERATOR', 'MAINTENANCE_TECHNICIAN')")
    public PageResponse<WorkOrderResponse> list(Long equipmentId, int page, int size) {
        var pageable = PageRequests.of(page, size, Sort.by("reportedAt", "id").descending());
        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow(() -> notFound("Equipment"));
        staffAccess.requireEquipmentAccess(equipment);
        return PageResponse.from(repository.findByEquipment_Id(equipmentId, pageable).map(mapper::toResponse));
    }

    private WorkOrderResponse finish(WorkOrder order, StaffMember actor, AuditAction action,
                                     EquipmentStatus previousStatus, String previousOrderStatus, String details) {
        audit.record(action, order.getEquipment(), order.getId(), actor, previousStatus,
                previousOrderStatus, order.getStatus().name(), details);
        // Flush both aggregates and the audit entry before returning their new version numbers.
        repository.flush();
        events.statusChanged(order.getEquipment(), previousStatus);
        return mapper.toResponse(order);
    }

    private WorkOrder requiredOrder(Long id) {
        return repository.findById(id).filter(WorkOrder::isActive).orElseThrow(() -> notFound("Work order"));
    }

    private Long hospitalId(WorkOrder order) {
        return order.getEquipment().getDepartment().getHospital().getId();
    }

    private void requireMutableEquipment(WorkOrder order) {
        if (!order.getEquipment().isActive() || !order.getEquipment().getDepartment().isActive()) {
            throw new WorkflowConflictException("Inactive equipment or departments cannot be repaired through this workflow");
        }
    }

    private void requireEquipmentStatus(WorkOrder order, EquipmentStatus expected) {
        if (order.getEquipment().getStatus() != expected) {
            throw new WorkflowConflictException("Equipment state does not match the requested repair action");
        }
    }

    private void requireVersion(Long expected, Long actual) {
        if (expected == null || !Objects.equals(expected, actual)) {
            throw new WorkflowConflictException("Stale version. Reload the record and submit its current version.");
        }
    }

    private ResponseStatusException notFound(String name) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, name + " not found");
    }
}
