package org.translator.healthlink.organization.dto;

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
            String code,

            @NotBlank
            @Size(max = 120)
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
            String code,

            @NotBlank
            @Size(max = 160)
            String name,

            @NotBlank
            @Size(max = 100)
            String city,

            @NotBlank
            @Size(max = 250)
            String address,

            @NotNull
            @Positive
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
            String code,

            @NotBlank
            @Size(max = 120)
            String name,

            @NotNull
            @Positive
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
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public PageResponse {
            items = List.copyOf(items);
        }
    }
}