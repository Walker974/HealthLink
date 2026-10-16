package org.aui.csc.monolith.healthlink.staff.service;

import lombok.RequiredArgsConstructor;
import org.aui.csc.monolith.healthlink.common.security.StaffAccess;
import org.aui.csc.monolith.healthlink.organization.entity.Department;
import org.aui.csc.monolith.healthlink.organization.entity.Hospital;
import org.aui.csc.monolith.healthlink.organization.repository.DepartmentRepository;
import org.aui.csc.monolith.healthlink.organization.repository.HospitalRepository;
import org.aui.csc.monolith.healthlink.staff.entity.StaffMember;
import org.aui.csc.monolith.healthlink.staff.entity.StaffRole;
import org.aui.csc.monolith.healthlink.staff.repository.StaffMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.aui.csc.monolith.healthlink.staff.dto.StaffDtos.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffService {
    private final StaffMemberRepository repository;
    private final HospitalRepository hospitals;
    private final DepartmentRepository departments;
    private final StaffAccess staffAccess;

    @Transactional
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public StaffResponse create(CreateStaffRequest request) {
        boolean hospitalRole = request.role() == StaffRole.HOSPITAL_MANAGER
                || request.role() == StaffRole.RADIOLOGY_OPERATOR
                || request.role() == StaffRole.MAINTENANCE_TECHNICIAN;
        if (hospitalRole != (request.hospitalId() != null)
                || (request.role() == StaffRole.RADIOLOGY_OPERATOR) != (request.departmentId() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hospital roles require a hospital; only radiology operators require a department");
        }
        Hospital hospital = request.hospitalId() == null ? null : hospitals.findById(request.hospitalId())
                .filter(Hospital::isActive).orElseThrow(() -> notFound("Hospital"));
        Department department = request.departmentId() == null ? null : departments.findById(request.departmentId())
                .filter(Department::isActive).orElseThrow(() -> notFound("Department"));
        if (department != null && !department.getHospital().getId().equals(hospital.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department must belong to the assigned hospital");
        }
        StaffMember staff = new StaffMember(request.keycloakSubject().strip(), request.fullName().strip(),
                request.email().strip(), request.role(), hospital, department);
        return toResponse(repository.saveAndFlush(staff));
    }

    @PreAuthorize("isAuthenticated()")
    public StaffResponse me() {
        return toResponse(staffAccess.currentStaff());
    }

    private StaffResponse toResponse(StaffMember staff) {
        return new StaffResponse(staff.getId(), staff.getKeycloakSubject(), staff.getFullName(), staff.getEmail(),
                staff.getRole(), staff.getHospital() == null ? null : staff.getHospital().getId(),
                staff.getDepartment() == null ? null : staff.getDepartment().getId());
    }

    private ResponseStatusException notFound(String name) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, name + " not found");
    }
}
