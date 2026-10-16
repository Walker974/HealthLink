package org.aui.csc.monolith.healthlink.staff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.aui.csc.monolith.healthlink.staff.entity.StaffRole;

public final class StaffDtos {
    private StaffDtos() {}

    public record CreateStaffRequest(
            @NotBlank @Size(max = 255) String keycloakSubject,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotNull StaffRole role,
            @Positive Long hospitalId,
            @Positive Long departmentId) {}

    public record StaffResponse(Long id, String keycloakSubject, String fullName,
                                String email, StaffRole role, Long hospitalId, Long departmentId) {}
}
