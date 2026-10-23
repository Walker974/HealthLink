package ma.aui.csc.monolith.healthlink.equipment.service;

import java.util.List;

import ma.aui.csc.monolith.healthlink.common.security.StaffAccess;
import ma.aui.csc.monolith.healthlink.equipment.data.EquipmentDtos.EquipmentResponse;
import ma.aui.csc.monolith.healthlink.equipment.data.EquipmentDtos.RegisterEquipmentRequest;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.Equipment;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.ImagingType;
import ma.aui.csc.monolith.healthlink.equipment.data.EquipmentMapper;
import ma.aui.csc.monolith.healthlink.equipment.notification.StatusChangeNotifier;
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
    private final StatusChangeNotifier notifier;   // an INTERFACE, not a concrete class
    private final StaffAccess staffAccess;
    private final EquipmentMapper mapper;
    public EquipmentService(EquipmentRepository equipmentRepository,
                            ImagingTypeRepository imagingTypeRepository,
                            DepartmentRepository departmentRepository,
                            StatusChangeNotifier notifier,
                            StaffAccess staffAccess,
                            EquipmentMapper mapper) {
        this.equipmentRepository = equipmentRepository;
        this.imagingTypeRepository = imagingTypeRepository;
        this.departmentRepository = departmentRepository;
        this.notifier = notifier;
        this.staffAccess = staffAccess;
        this.mapper = mapper;
    }

    @Transactional
    @PreAuthorize("hasRole('HOSPITAL_MANAGER')")
    public EquipmentResponse register(RegisterEquipmentRequest request) {
        Department department = departmentRepository.findById(request.departmentId())
                .filter(Department::isActive)
                .orElseThrow(() -> notFound("Department"));
        staffAccess.requireHospitalManager(department.getHospital().getId());

        ImagingType type = imagingTypeRepository.findByCode(request.imagingTypeCode())
                .orElseThrow(() -> notFound("Imaging type"));

        Equipment equipment = new Equipment(
                request.serialNumber().strip(),
                request.model().strip(),
                department,
                type
        );
        // saveAndFlush: send the INSERT now, so a duplicate serial number fails HERE
        // (and becomes a 409 in EquipmentExceptionHandler), not later at commit.
        return mapper.toResponse(equipmentRepository.saveAndFlush(equipment));
    }

    @Transactional
    @PreAuthorize("hasRole('RADIOLOGY_OPERATOR')")
    public EquipmentResponse changeStatus(Long equipmentId, EquipmentStatus newStatus) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .filter(Equipment::isActive)
                .orElseThrow(() -> notFound("Equipment"));
        staffAccess.requireDepartmentOperator(equipment.getDepartment());
        if (newStatus != EquipmentStatus.OUT_OF_SERVICE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only fault reporting is supported here; repair and verification require a work order");
        }

        EquipmentStatus previous = equipment.getStatus();
        if (previous == newStatus) {
            return mapper.toResponse(equipment);
        }

        equipment.changeStatus(newStatus);

        notifier.statusChanged(equipment, previous);

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
