package ma.aui.csc.monolith.healthlink.equipment.notification;

import java.time.Instant;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;

public record EquipmentStatusChanged(Long equipmentId, EquipmentStatus previousStatus,
                                     EquipmentStatus status, Instant statusUpdatedAt) {}
