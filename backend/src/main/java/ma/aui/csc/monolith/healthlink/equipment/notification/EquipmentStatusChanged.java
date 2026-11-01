package ma.aui.csc.monolith.healthlink.equipment.notification;

import java.time.Instant;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;

/**
 * Immutable snapshot of one committed status change. It carries only technical identifiers
 * (no personal data), so it is safe to log, cache-invalidate on and broadcast to the public map.
 */
public record EquipmentStatusChanged(Long equipmentId, EquipmentStatus previousStatus,
                                     EquipmentStatus status, Instant statusUpdatedAt,
                                     Long hospitalId, String imagingTypeCode) {}
