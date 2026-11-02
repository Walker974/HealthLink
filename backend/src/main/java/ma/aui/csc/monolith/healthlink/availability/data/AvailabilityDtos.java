package ma.aui.csc.monolith.healthlink.availability.data;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;
import ma.aui.csc.monolith.healthlink.equipment.notification.EquipmentStatusChanged;

public final class AvailabilityDtos {
    private AvailabilityDtos() {}

    /** One hospital that currently has at least one working machine of the requested type. */
    public record NearbyHospitalResponse(
            Long hospitalId,
            String hospitalName,
            String city,
            String address,
            Double latitude,
            Double longitude,
            @Schema(description = "Approximate road-independent distance in kilometres (about 1 km precision)")
            double distanceKm,
            @Schema(description = "Number of working machines of the requested type in this hospital")
            long availableCount,
            @Schema(description = "Most recent status change among those machines")
            Instant lastStatusChange) {}

    /**
     * What the public map receives over Server-Sent Events. Technical identifiers only:
     * no staff names, notes or patient data ever leave the server through this channel.
     */
    public record LiveAvailabilityEvent(
            Long equipmentId,
            Long hospitalId,
            String imagingTypeCode,
            EquipmentStatus previousStatus,
            EquipmentStatus status,
            Instant statusUpdatedAt,
            @Schema(description = "Application instance that committed the change (useful to demonstrate Redis fan-out)")
            String publishedBy) {

        public static LiveAvailabilityEvent from(EquipmentStatusChanged change, String instanceId) {
            return new LiveAvailabilityEvent(change.equipmentId(), change.hospitalId(), change.imagingTypeCode(),
                    change.previousStatus(), change.status(), change.statusUpdatedAt(), instanceId);
        }
    }
}
