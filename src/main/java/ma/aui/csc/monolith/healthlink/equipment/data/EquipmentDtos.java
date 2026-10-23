package ma.aui.csc.monolith.healthlink.equipment.data;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;

public final class EquipmentDtos {

    private EquipmentDtos() {
    }

    public record RegisterEquipmentRequest(
            @Schema(description = "Unique equipment serial number", example = "DEMO-MRI-001")
            @NotBlank @Size(max = 60) String serialNumber,
            @Schema(example = "Demo MRI model")
            @NotBlank @Size(max = 120) String model,
            @Schema(description = "Active department in the manager's assigned hospital", example = "1")
            @NotNull @Positive Long departmentId,
            @Schema(description = "Code of an existing imaging type", example = "MRI")
            @NotBlank @Size(max = 30) String imagingTypeCode
    ) {
    }

    public record ChangeStatusRequest(
            @Schema(description = "Only OUT_OF_SERVICE is accepted by the fault-reporting endpoint. "
                    + "Other equipment states require the future repair verification workflow.",
                    example = "OUT_OF_SERVICE", allowableValues = {"OUT_OF_SERVICE"})
            @NotNull EquipmentStatus status
    ) {
    }

    public record EquipmentResponse(
            Long id,
            String serialNumber,
            String model,
            EquipmentStatus status,
            Instant statusUpdatedAt,
            Long departmentId,
            String imagingTypeCode
    ) {
    }
}
