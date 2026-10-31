package ma.aui.csc.monolith.healthlink.audit.data.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.Equipment;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;
import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffMember;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "audit_records")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditRecord {
    @Id
    @Column(updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private AuditAction action;
    @Column(nullable = false, updatable = false)
    private Long equipmentId;
    @Column(nullable = false, updatable = false)
    private Long hospitalId;
    @Column(updatable = false)
    private Long workOrderId;
    @Column(nullable = false, length = 255, updatable = false)
    private String actorSubject;
    @Column(nullable = false, updatable = false)
    private Long actorStaffId;
    @Column(nullable = false, length = 40, updatable = false)
    private String actorRole;
    @Enumerated(EnumType.STRING)
    @Column(length = 30, updatable = false)
    private EquipmentStatus previousEquipmentStatus;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private EquipmentStatus equipmentStatus;
    @Column(length = 40, updatable = false)
    private String previousWorkOrderStatus;
    @Column(length = 40, updatable = false)
    private String workOrderStatus;
    @Column(nullable = false, length = 2000, updatable = false)
    private String details;

    public AuditRecord(AuditAction action, Equipment equipment, Long workOrderId, StaffMember actor,
                       EquipmentStatus previousEquipmentStatus, String previousWorkOrderStatus,
                       String workOrderStatus, String details) {
        this.id = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.action = action;
        this.equipmentId = equipment.getId();
        this.hospitalId = equipment.getDepartment().getHospital().getId();
        this.workOrderId = workOrderId;
        this.actorSubject = actor.getKeycloakSubject();
        this.actorStaffId = actor.getId();
        this.actorRole = actor.getRole().name();
        this.previousEquipmentStatus = previousEquipmentStatus;
        this.equipmentStatus = equipment.getStatus();
        this.previousWorkOrderStatus = previousWorkOrderStatus;
        this.workOrderStatus = workOrderStatus;
        this.details = details.strip();
    }
}
