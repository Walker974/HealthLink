package org.aui.csc.monolith.healthlink.equipment.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.aui.csc.monolith.healthlink.equipment.entity.EquipmentStatus;

public final class EquipmentDtos {

    private EquipmentDtos() {
    }

    public record RegisterEquipmentRequest(
            @NotBlank @Size(max = 60) String serialNumber,
            @NotBlank @Size(max = 120) String model,
            @NotNull @Positive Long departmentId,
            @NotBlank @Size(max = 30) String imagingTypeCode
    ) {
    }

    public record ChangeStatusRequest(
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
