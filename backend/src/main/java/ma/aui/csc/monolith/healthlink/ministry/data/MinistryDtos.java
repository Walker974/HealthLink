package ma.aui.csc.monolith.healthlink.ministry.data;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public final class MinistryDtos {
    private MinistryDtos() {}

    public record AvailabilityFigures(
            long hospitals,
            long totalMachines,
            long available,
            long outOfService,
            long underMaintenance,
            @Schema(description = "Share of machines that are available, in percent with one decimal; null when there are no machines")
            Double availabilityPercent) {}

    public record RegionAvailability(Long regionId, String code, String name, AvailabilityFigures figures) {}

    public record AvailabilitySummaryResponse(
            @Schema(description = "Imaging type filter, or null for all types", example = "MRI")
            String imagingType,
            Instant generatedAt,
            AvailabilityFigures national,
            List<RegionAvailability> regions) {}
}
