package ma.aui.csc.monolith.healthlink.equipment.data;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ImagingTypeDtos {
    private ImagingTypeDtos() {}

    public record CreateImagingTypeRequest(
            @NotBlank @Size(max = 30)
            @Pattern(regexp = "[A-Z0-9_-]+", message = "Use uppercase letters, digits, underscores or hyphens")
            @Schema(description = "Unique code used in searches and URLs", example = "PET_CT")
            String code,
            @NotBlank @Size(max = 120)
            @Schema(example = "PET-CT")
            String name) {}

    public record ImagingTypeResponse(Long id, String code, String name) {}
}
