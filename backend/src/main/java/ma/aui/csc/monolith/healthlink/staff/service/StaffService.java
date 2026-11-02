package ma.aui.csc.monolith.healthlink.staff.service;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.common.data.PageRequests;
import ma.aui.csc.monolith.healthlink.common.data.PageResponse;
import ma.aui.csc.monolith.healthlink.common.security.StaffAccess;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Department;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Hospital;
import ma.aui.csc.monolith.healthlink.organization.data.repository.DepartmentRepository;
import ma.aui.csc.monolith.healthlink.organization.data.repository.HospitalRepository;
import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffMember;
import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffRole;
import ma.aui.csc.monolith.healthlink.staff.data.StaffMapper;
import ma.aui.csc.monolith.healthlink.staff.data.repository.StaffMemberRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static ma.aui.csc.monolith.healthlink.staff.data.StaffDtos.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffService {
    private final StaffMemberRepository repository;
    private final HospitalRepository hospitals;
    private final DepartmentRepository departments;
    private final StaffAccess staffAccess;
    private final StaffMapper mapper;

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
        return mapper.toResponse(repository.saveAndFlush(staff));
    }

    /**
     * PLATFORM_ADMIN sees every staff profile (optionally filtered by hospital);
     * a HOSPITAL_MANAGER sees only the staff of their own hospital.
     */
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'HOSPITAL_MANAGER')")
    public PageResponse<StaffResponse> list(Long hospitalId, int page, int size) {
        var pageable = PageRequests.of(page, size, Sort.by("id").ascending());
        if (staffAccess.hasRole(StaffRole.PLATFORM_ADMIN)) {
            var result = hospitalId == null ? repository.findAll(pageable) : repository.findByHospital_Id(hospitalId, pageable);
            return PageResponse.from(result.map(mapper::toResponse));
        }
        StaffMember manager = staffAccess.currentStaff();
        if (manager.getHospital() == null) {
            throw new AccessDeniedException("A hospital assignment is required");
        }
        Long ownHospital = manager.getHospital().getId();
        if (hospitalId != null && !hospitalId.equals(ownHospital)) {
            throw new AccessDeniedException("Staff of other hospitals is not accessible");
        }
        staffAccess.requireHospitalManager(ownHospital);
        return PageResponse.from(repository.findByHospital_Id(ownHospital, pageable).map(mapper::toResponse));
    }

    /**
     * Deactivation is a soft delete: the profile and all history stay, but the person can no longer
     * use staff operations. Managers may deactivate operators and technicians of their own hospital;
     * only a platform administrator may deactivate a manager. Nobody can deactivate themselves.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'HOSPITAL_MANAGER')")
    public StaffResponse deactivate(Long staffId) {
        StaffMember target = manageable(staffId);
        target.deactivate();
        return mapper.toResponse(repository.saveAndFlush(target));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'HOSPITAL_MANAGER')")
    public StaffResponse activate(Long staffId) {
        StaffMember target = manageable(staffId);
        target.activate();
        return mapper.toResponse(repository.saveAndFlush(target));
    }

    private StaffMember manageable(Long staffId) {
        StaffMember target = repository.findById(staffId).orElseThrow(() -> notFound("Staff member"));
        String callerSubject = currentSubject();
        if (callerSubject != null && Objects.equals(callerSubject, target.getKeycloakSubject())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot change your own access");
        }
        if (staffAccess.hasRole(StaffRole.PLATFORM_ADMIN)) {
            return target;
        }
        StaffMember manager = staffAccess.currentStaff();
        boolean sameHospital = manager.getHospital() != null && target.getHospital() != null
                && Objects.equals(manager.getHospital().getId(), target.getHospital().getId());
        boolean hospitalWorker = target.getRole() == StaffRole.RADIOLOGY_OPERATOR
                || target.getRole() == StaffRole.MAINTENANCE_TECHNICIAN;
        if (!sameHospital || !hospitalWorker) {
            throw new AccessDeniedException("Managers can only manage operators and technicians of their hospital");
        }
        staffAccess.requireHospitalManager(manager.getHospital().getId());
        return target;
    }

    private String currentSubject() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken().getSubject();
        }
        return null;
    }

    @PreAuthorize("isAuthenticated()")
    public StaffResponse me() {
        return mapper.toResponse(staffAccess.currentStaff());
    }

    private ResponseStatusException notFound(String name) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, name + " not found");
    }
}
