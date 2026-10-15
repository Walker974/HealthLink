package org.aui.csc.monolith.healthlink.equipment.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.aui.csc.monolith.healthlink.equipment.entity.EquipmentStatus;

/**
 * What the client may SEND (requests) and what we SEND BACK (responses).
 * Records = small immutable data classes (Java writes constructor + getters).
 */
public final class EquipmentDtos {

    private EquipmentDtos() {
    }

    // IN: register a new machine. No id, no status: the server decides those.
    public record RegisterEquipmentRequest(
            @NotBlank @Size(max = 60) String serialNumber,
            @NotBlank @Size(max = 120) String model,
            @NotNull @Positive Long departmentId,
            @NotBlank @Size(max = 30) String imagingTypeCode
    ) {
    }

    // IN: change the status of a machine.
    public record ChangeStatusRequest(
            @NotNull EquipmentStatus status
    ) {
    }

    // OUT: what the client sees. Only safe, useful fields.
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
