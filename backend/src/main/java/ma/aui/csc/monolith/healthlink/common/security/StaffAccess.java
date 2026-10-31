package ma.aui.csc.monolith.healthlink.common.security;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Department;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.Equipment;
import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffMember;
import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffRole;
import ma.aui.csc.monolith.healthlink.staff.data.repository.StaffMemberRepository;
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
                .filter(StaffMember::isActive)
                .orElseThrow(StaffAccess::denied);
        boolean matchingRole = token.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + staff.getRole().name()));
        if (!matchingRole) {
            throw denied();
        }
        return staff;
    }

    public StaffMember requireHospitalManager(Long hospitalId) {
        return requireHospitalRole(hospitalId, StaffRole.HOSPITAL_MANAGER);
    }

    public StaffMember requireHospitalRole(Long hospitalId, StaffRole role) {
        StaffMember staff = currentStaff();
        if (staff.getRole() != role || !inHospital(staff, hospitalId)) {
            throw denied();
        }
        return staff;
    }

    public void requireHospitalStaff(Long hospitalId) {
        StaffMember staff = currentStaff();
        if (!inHospital(staff, hospitalId)) {
            throw denied();
        }
    }

    public StaffMember requireDepartmentOperator(Department department) {
        StaffMember staff = currentStaff();
        if (staff.getRole() != StaffRole.RADIOLOGY_OPERATOR
                || staff.getDepartment() == null || !staff.getDepartment().isActive()
                || !Objects.equals(staff.getDepartment().getId(), department.getId())
                || !department.isActive() || !inHospital(staff, department.getHospital().getId())) {
            throw denied();
        }
        return staff;
    }

    public StaffMember requireEquipmentAccess(Equipment equipment) {
        StaffMember staff = currentStaff();
        if (staff.getRole() == StaffRole.RADIOLOGY_OPERATOR) {
            if (staff.getDepartment() == null || !staff.getDepartment().isActive()
                    || !equipment.getDepartment().isActive()
                    || !Objects.equals(staff.getDepartment().getId(), equipment.getDepartment().getId())
                    || !inHospital(staff, equipment.getDepartment().getHospital().getId())) {
                throw denied();
            }
        } else if ((staff.getRole() != StaffRole.HOSPITAL_MANAGER && staff.getRole() != StaffRole.MAINTENANCE_TECHNICIAN)
                || !inHospital(staff, equipment.getDepartment().getHospital().getId())) {
            throw denied();
        }
        return staff;
    }

    private boolean inHospital(StaffMember staff, Long hospitalId) {
        return hospitalId != null && staff.getHospital() != null && staff.getHospital().isActive()
                && Objects.equals(staff.getHospital().getId(), hospitalId);
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("An active staff assignment with permission for this organization is required");
    }
}
