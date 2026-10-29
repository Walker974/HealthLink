package ma.aui.csc.monolith.healthlink.maintenance.data.entity;

import java.time.Instant;
import java.util.Objects;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ma.aui.csc.monolith.healthlink.common.entity.BaseEntity;
import ma.aui.csc.monolith.healthlink.common.error.WorkflowConflictException;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.Equipment;
import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffMember;
import org.springframework.security.access.AccessDeniedException;

@Entity
@Table(name = "work_orders", uniqueConstraints = @UniqueConstraint(
        name = "uk_work_orders_open_equipment", columnNames = "open_equipment_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private StaffMember reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repairer_id")
    private StaffMember repairer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verifier_id")
    private StaffMember verifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WorkOrderStatus status;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 2000)
    private String repairNotes;

    @Column(length = 2000)
    private String verificationNotes;

    @Column(nullable = false)
    private Instant reportedAt;
    private Instant acceptedAt;
    private Instant completedAt;
    private Instant reviewedAt;
    private Instant verifiedAt;

    // A unique, nullable slot: every unverified order reserves its equipment ID.
    @Column(name = "open_equipment_id")
    private Long openEquipmentId;

    public WorkOrder(Equipment equipment, StaffMember reporter, String description) {
        this.equipment = Objects.requireNonNull(equipment);
        this.reporter = Objects.requireNonNull(reporter);
        this.description = description.strip();
        this.status = WorkOrderStatus.OPEN;
        this.openEquipmentId = Objects.requireNonNull(equipment.getId());
        this.reportedAt = Instant.now();
    }

    public void accept(StaffMember technician) {
        requireStatus(WorkOrderStatus.OPEN);
        this.repairer = technician;
        this.acceptedAt = Instant.now();
        this.status = WorkOrderStatus.IN_PROGRESS;
    }

    public void complete(StaffMember technician, String notes) {
        requireStatus(WorkOrderStatus.IN_PROGRESS);
        if (!Objects.equals(repairer.getId(), technician.getId())) {
            throw new AccessDeniedException("Only the assigned technician may complete this repair");
        }
        this.repairNotes = notes.strip();
        this.completedAt = Instant.now();
        this.status = WorkOrderStatus.AWAITING_VERIFICATION;
    }

    public void verify(StaffMember manager, String notes) {
        review(manager, notes);
        this.status = WorkOrderStatus.VERIFIED;
        this.verifiedAt = reviewedAt;
        this.openEquipmentId = null;
    }

    public void reject(StaffMember manager, String reason) {
        review(manager, reason);
        this.status = WorkOrderStatus.IN_PROGRESS;
        this.completedAt = null;
    }

    private void review(StaffMember manager, String notes) {
        requireStatus(WorkOrderStatus.AWAITING_VERIFICATION);
        if (Objects.equals(repairer.getId(), manager.getId())
                || Objects.equals(repairer.getKeycloakSubject(), manager.getKeycloakSubject())) {
            throw new AccessDeniedException("A repairer cannot verify or reject their own repair");
        }
        this.verifier = manager;
        this.verificationNotes = notes.strip();
        this.reviewedAt = Instant.now();
    }

    private void requireStatus(WorkOrderStatus expected) {
        if (status != expected) {
            throw new WorkflowConflictException("This action requires " + expected + "; current status is " + status);
        }
    }
}
