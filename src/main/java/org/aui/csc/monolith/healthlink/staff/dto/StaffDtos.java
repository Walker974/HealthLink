package org.aui.csc.monolith.healthlink.staff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.aui.csc.monolith.healthlink.staff.entity.StaffRole;

public final class StaffDtos {
    private StaffDtos() {}

    public record CreateStaffRequest(
            @Schema(description = "Immutable Keycloak user ID (JWT sub), not the username or email",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @NotBlank @Size(max = 255) String keycloakSubject,
            @Schema(example = "Demo Manager")
            @NotBlank @Size(max = 120) String fullName,
            @Schema(example = "manager@example.test")
            @NotBlank @Email @Size(max = 254) String email,
            @Schema(description = "Must match the user's separately assigned Keycloak realm role", example = "HOSPITAL_MANAGER")
            @NotNull StaffRole role,
            @Schema(description = "Required for manager, operator and technician; omit for admin and ministry roles", example = "1")
            @Positive Long hospitalId,
            @Schema(description = "Required only for operators; must belong to the assigned hospital. Omit for other roles.")
            @Positive Long departmentId) {}

    public record StaffResponse(Long id, String keycloakSubject, String fullName,
                                String email, StaffRole role, Long hospitalId, Long departmentId) {}
}
