package ma.aui.csc.monolith.healthlink.audit.service;

import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.audit.data.AuditDtos.AuditResponse;
import ma.aui.csc.monolith.healthlink.audit.data.AuditMapper;
import ma.aui.csc.monolith.healthlink.audit.data.entity.*;
import ma.aui.csc.monolith.healthlink.audit.data.repository.AuditRecordRepository;
import ma.aui.csc.monolith.healthlink.common.data.*;
import ma.aui.csc.monolith.healthlink.common.security.StaffAccess;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.*;
import ma.aui.csc.monolith.healthlink.equipment.data.repository.EquipmentRepository;
import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffMember;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {
    private final AuditRecordRepository repository;
    private final AuditMapper mapper;
    private final EquipmentRepository equipmentRepository;
    private final StaffAccess staffAccess;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditAction action, Equipment equipment, Long workOrderId, StaffMember actor,
                       EquipmentStatus previousStatus, String previousOrderStatus, String orderStatus, String details) {
        repository.append(new AuditRecord(action, equipment, workOrderId, actor,
                previousStatus, previousOrderStatus, orderStatus, details));
    }

    @PreAuthorize("hasAnyRole('HOSPITAL_MANAGER', 'RADIOLOGY_OPERATOR', 'MAINTENANCE_TECHNICIAN')")
    public PageResponse<AuditResponse> history(Long equipmentId, int page, int size) {
        var pageable = PageRequests.of(page, size, Sort.unsorted());
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));
        staffAccess.requireEquipmentAccess(equipment);
        return PageResponse.from(repository.findByEquipmentId(equipmentId, pageable).map(mapper::toResponse));
    }
}
