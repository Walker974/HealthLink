package org.aui.csc.monolith.healthlink.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.aui.csc.monolith.healthlink.staff.entity.StaffMember;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.jpa.domain.AbstractAuditable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Common parent of every HealthLink entity.
 *
 * - @MappedSuperclass: this class has NO table of its own;
 *   its fields are copied into each child's table.
 * - abstract: nobody can create a "BaseEntity" directly, only its children.
 *
 * Inheritance chain:
 *   AbstractPersistable<Long>               -> id (primary key)
 *     AbstractAuditable<StaffMember, Long>  -> createdBy, createdDate,
 *                                              lastModifiedBy, lastModifiedDate
 *       BaseEntity                          -> active (soft delete)
 *         Region, Hospital, Department, StaffMember, ...
 */
@MappedSuperclass
// "Before every INSERT/UPDATE, let Spring's auditing listener fill the audit fields."
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity extends AbstractAuditable<StaffMember, Long> {

    // The id is inherited from Spring Data (AbstractPersistable): written once, for all (DRY).

    // Soft delete flag: we never DELETE rows, we set active = false
    // so the history (equipment, status changes) is never lost.
    // @ColumnDefault("true"): existing rows get "true" when Hibernate adds the column.
    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean active = true;

    // No setters: state changes only through these explicit methods.
    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
