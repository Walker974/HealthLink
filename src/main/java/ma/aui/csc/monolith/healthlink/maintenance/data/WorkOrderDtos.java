package ma.aui.csc.monolith.healthlink.maintenance.data;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;
import ma.aui.csc.monolith.healthlink.maintenance.data.entity.WorkOrderStatus;

public final class WorkOrderDtos {
    private WorkOrderDtos() {}

    public record ReportFaultRequest(
            @NotNull @Positive @Schema(example = "1") Long equipmentId,
            @NotNull @PositiveOrZero @Schema(description = "Current equipment version from GET /api/equipment/{id}", example = "0") Long equipmentVersion,
            @NotBlank @Size(max = 2000) @Schema(example = "Scanner fails its startup self-test") String description) {}

    public record VersionRequest(
            @NotNull @PositiveOrZero @Schema(description = "Current work-order version", example = "0") Long version) {}

    public record CompleteRepairRequest(
            @NotNull @PositiveOrZero Long version,
            @NotBlank @Size(max = 2000) @Schema(example = "Replaced faulty power supply; startup test passes") String repairNotes) {}

    public record ReviewRepairRequest(
            @NotNull @PositiveOrZero Long version,
            @NotBlank @Size(max = 2000) @Schema(description = "Verification findings, or the reason for rejection",
                    example = "Independent operational checks passed") String notes) {}

    public record WorkOrderResponse(Long id, Long version, Long equipmentId, Long equipmentVersion,
            EquipmentStatus equipmentStatus, Long hospitalId, Long departmentId, WorkOrderStatus status,
            String description, String repairNotes, String verificationNotes,
            Long reporterId, Long repairerId, Long verifierId,
            Instant reportedAt, Instant acceptedAt, Instant completedAt, Instant reviewedAt, Instant verifiedAt) {}
}
