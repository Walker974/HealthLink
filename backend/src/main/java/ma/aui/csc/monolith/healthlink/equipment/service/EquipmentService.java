package ma.aui.csc.monolith.healthlink.equipment.service;

import java.util.List;

import ma.aui.csc.monolith.healthlink.common.security.StaffAccess;
import ma.aui.csc.monolith.healthlink.equipment.data.EquipmentDtos.EquipmentResponse;
import ma.aui.csc.monolith.healthlink.equipment.data.EquipmentDtos.RegisterEquipmentRequest;
import ma.aui.csc.monolith.healthlink.equipment.data.EquipmentDtos.ChangeStatusRequest;
import ma.aui.csc.monolith.healthlink.audit.data.entity.AuditAction;
import ma.aui.csc.monolith.healthlink.audit.service.AuditService;
import ma.aui.csc.monolith.healthlink.maintenance.data.WorkOrderDtos.ReportFaultRequest;
import ma.aui.csc.monolith.healthlink.maintenance.service.WorkOrderService;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.Equipment;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.ImagingType;
import ma.aui.csc.monolith.healthlink.equipment.data.EquipmentMapper;
import ma.aui.csc.monolith.healthlink.equipment.notification.EquipmentEvents;
import ma.aui.csc.monolith.healthlink.equipment.data.repository.EquipmentRepository;
import ma.aui.csc.monolith.healthlink.equipment.data.repository.ImagingTypeRepository;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Department;
import ma.aui.csc.monolith.healthlink.organization.data.repository.DepartmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * BUSINESS SERVICE for equipment: the rules live here.
 *
 * @Service: Spring creates ONE EquipmentService at startup (a bean)
 * and gives it to whoever needs it (the future EquipmentController).
 *
 * @Transactional(readOnly = true): every method runs inside a database
 * transaction (all-or-nothing). Write methods override it below.
 */
@Service
@Transactional(readOnly = true)
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final ImagingTypeRepository imagingTypeRepository;
    private final DepartmentRepository departmentRepository;
    private final EquipmentEvents events;
    private final AuditService audit;
    private final WorkOrderService workOrders;
    private final StaffAccess staffAccess;
    private final EquipmentMapper mapper;
    public EquipmentService(EquipmentRepository equipmentRepository,
                            ImagingTypeRepository imagingTypeRepository,
                            DepartmentRepository departmentRepository,
                            EquipmentEvents events,
                            AuditService audit,
                            WorkOrderService workOrders,
                            StaffAccess staffAccess,
                            EquipmentMapper mapper) {
        this.equipmentRepository = equipmentRepository;
        this.imagingTypeRepository = imagingTypeRepository;
        this.departmentRepository = departmentRepository;
        this.events = events;
        this.audit = audit;
        this.workOrders = workOrders;
        this.staffAccess = staffAccess;
        this.mapper = mapper;
    }

    @Transactional
    @PreAuthorize("hasRole('HOSPITAL_MANAGER')")
    public EquipmentResponse register(RegisterEquipmentRequest request) {
        Department department = departmentRepository.findById(request.departmentId())
                .filter(Department::isActive)
                .orElseThrow(() -> notFound("Department"));
        var actor = staffAccess.requireHospitalManager(department.getHospital().getId());

        ImagingType type = imagingTypeRepository.findByCode(request.imagingTypeCode())
                .filter(ImagingType::isActive)
                .orElseThrow(() -> notFound("Imaging type"));

        Equipment equipment = new Equipment(
                request.serialNumber().strip(),
                request.model().strip(),
                department,
                type
        );
        equipmentRepository.save(equipment);
        audit.record(AuditAction.EQUIPMENT_REGISTERED, equipment, null, actor,
                null, null, null, "Equipment registered");
        equipmentRepository.flush();
        events.statusChanged(equipment, null);
        return mapper.toResponse(equipment);
    }

    @Transactional
    @PreAuthorize("hasRole('RADIOLOGY_OPERATOR')")
    public EquipmentResponse changeStatus(Long equipmentId, ChangeStatusRequest request) {
        if (request.status() != EquipmentStatus.OUT_OF_SERVICE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use the work-order workflow for repair and verification");
        }
        workOrders.reportFault(new ReportFaultRequest(equipmentId, request.version(), request.description()));
        return mapper.toResponse(equipmentRepository.findById(equipmentId).orElseThrow(() -> notFound("Equipment")));
    }

    @PreAuthorize("hasAnyRole('HOSPITAL_MANAGER', 'RADIOLOGY_OPERATOR', 'MAINTENANCE_TECHNICIAN')")
    public EquipmentResponse get(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow(() -> notFound("Equipment"));
        staffAccess.requireEquipmentAccess(equipment);
        return mapper.toResponse(equipment);
    }

    public List<EquipmentResponse> findAvailable(String imagingTypeCode, String city) {
        return equipmentRepository
                .findByStatusAndTypeInCity(EquipmentStatus.AVAILABLE, imagingTypeCode, city)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    private ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
    }
}
