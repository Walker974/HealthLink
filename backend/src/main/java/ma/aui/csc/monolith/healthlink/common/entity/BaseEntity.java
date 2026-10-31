package ma.aui.csc.monolith.healthlink.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffMember;
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
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity extends AbstractAuditable<StaffMember, Long> {

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    // The id is inherited from Spring Data (AbstractPersistable): written once, for all (DRY).

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean active = true;

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
