package org.aui.csc.monolith.healthlink.common.security;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.aui.csc.monolith.healthlink.organization.entity.Department;
import org.aui.csc.monolith.healthlink.staff.entity.StaffMember;
import org.aui.csc.monolith.healthlink.staff.entity.StaffRole;
import org.aui.csc.monolith.healthlink.staff.repository.StaffMemberRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StaffAccess {

    private final StaffMemberRepository repository;

    public StaffMember currentStaff() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
            throw denied();
        }
        if (token.getToken().getSubject() == null || token.getToken().getSubject().isBlank()) {
            throw denied();
        }
        StaffMember staff = repository.findByKeycloakSubjectAndActiveTrue(token.getToken().getSubject())
                .orElseThrow(StaffAccess::denied);
        boolean matchingRole = token.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + staff.getRole().name()));
        if (!matchingRole) {
            throw denied();
        }
        return staff;
    }

    public void requireHospitalManager(Long hospitalId) {
        StaffMember staff = currentStaff();
        if (staff.getRole() != StaffRole.HOSPITAL_MANAGER || !inHospital(staff, hospitalId)) {
            throw denied();
        }
    }

    public void requireHospitalStaff(Long hospitalId) {
        StaffMember staff = currentStaff();
        if (!inHospital(staff, hospitalId)) {
            throw denied();
        }
    }

    public void requireDepartmentOperator(Department department) {
        StaffMember staff = currentStaff();
        if (staff.getRole() != StaffRole.RADIOLOGY_OPERATOR
                || staff.getDepartment() == null || !staff.getDepartment().isActive()
                || !Objects.equals(staff.getDepartment().getId(), department.getId())
                || !department.isActive() || !inHospital(staff, department.getHospital().getId())) {
            throw denied();
        }
    }

    private boolean inHospital(StaffMember staff, Long hospitalId) {
        return hospitalId != null && staff.getHospital() != null && staff.getHospital().isActive()
                && Objects.equals(staff.getHospital().getId(), hospitalId);
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("An active staff assignment with permission for this organization is required");
    }
}
