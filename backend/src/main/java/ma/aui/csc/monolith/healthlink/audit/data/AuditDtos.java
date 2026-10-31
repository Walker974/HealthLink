package ma.aui.csc.monolith.healthlink.audit.data;

import java.time.Instant;
import java.util.UUID;
import ma.aui.csc.monolith.healthlink.audit.data.entity.AuditAction;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;

public final class AuditDtos {
    private AuditDtos() {}

    public record AuditResponse(UUID id, Instant occurredAt, AuditAction action,
            Long equipmentId, Long hospitalId, Long workOrderId,
            String actorSubject, Long actorStaffId, String actorRole,
            EquipmentStatus previousEquipmentStatus, EquipmentStatus equipmentStatus,
            String previousWorkOrderStatus, String workOrderStatus, String details) {}
}
