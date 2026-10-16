package org.aui.csc.monolith.healthlink.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class OrganizationDtos {

    private OrganizationDtos() {
    }

    public record CreateRegionRequest(
            @NotBlank
            @Size(max = 30)
            @Pattern(
                    regexp = "[A-Z0-9_-]+",
                    message = "Use uppercase letters, digits, underscores or hyphens"
            )
            @Schema(description = "Unique region code", example = "FES_MEKNES")
            String code,

            @NotBlank
            @Size(max = 120)
            @Schema(example = "Fes-Meknes")
            String name
    ) {
    }

    public record CreateHospitalRequest(
            @NotBlank
            @Size(max = 30)
            @Pattern(
                    regexp = "[A-Z0-9_-]+",
                    message = "Use uppercase letters, digits, underscores or hyphens"
            )
            @Schema(description = "Unique hospital code", example = "DEMO_FES")
            String code,

            @NotBlank
            @Size(max = 160)
            @Schema(example = "Demo Hospital")
            String name,

            @NotBlank
            @Size(max = 100)
            @Schema(example = "Fes")
            String city,

            @NotBlank
            @Size(max = 250)
            @Schema(example = "Demo address")
            String address,

            @NotNull
            @Positive
            @Schema(description = "ID of an existing region", example = "1")
            Long regionId
    ) {
    }

    public record CreateDepartmentRequest(
            @NotBlank
            @Size(max = 30)
            @Pattern(
                    regexp = "[A-Z0-9_-]+",
                    message = "Use uppercase letters, digits, underscores or hyphens"
            )
            @Schema(description = "Code unique within the hospital", example = "RADIOLOGY")
            String code,

            @NotBlank
            @Size(max = 120)
            @Schema(example = "Radiology")
            String name,

            @NotNull
            @Positive
            @Schema(description = "ID of the manager's assigned hospital", example = "1")
            Long hospitalId
    ) {
    }

    public record RegionResponse(
            Long id,
            String code,
            String name
    ) {
    }

    public record HospitalResponse(
            Long id,
            String code,
            String name,
            String city,
            String address,
            Long regionId
    ) {
    }

    public record DepartmentResponse(
            Long id,
            String code,
            String name,
            Long hospitalId
    ) {
    }

    public record PageResponse<T>(
            List<T> items,
            @Schema(description = "Zero-based page index", example = "0")
            int page,
            @Schema(description = "Requested page size", example = "20")
            int size,
            @Schema(description = "Total number of matching records", example = "1")
            long totalElements,
            @Schema(description = "Total number of pages", example = "1")
            int totalPages
    ) {
        public PageResponse {
            items = List.copyOf(items);
        }
    }
}
