package org.aui.csc.monolith.healthlink.equipment.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.aui.csc.monolith.healthlink.common.entity.BaseEntity;
import org.aui.csc.monolith.healthlink.organization.entity.Department;

/**
 * One physical machine (e.g. the MRI scanner of the radiology department
 * of CHU Hassan II) and its current operational status.
 * This is the heart of HealthLink: citizens search for AVAILABLE equipment.
 */
@Entity
@Table(name = "equipment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Equipment extends BaseEntity {   // inherits id, active, audit fields

    // Manufacturer serial number: identifies the physical machine, so it is unique.
    @Column(nullable = false, unique = true, length = 60)
    private String serialNumber;

    // Model name, e.g. "Siemens Magnetom Altea".
    @Column(nullable = false, length = 120)
    private String model;

    // Store the enum NAME ("AVAILABLE"), never its position number.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EquipmentStatus status;

    // When the status last changed: shown to citizens as "last update".
    @Column(nullable = false)
    private Instant statusUpdatedAt;

    // Many machines -> one department. Required: every machine belongs somewhere.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // Many machines -> one imaging type. Required: we must know what it is (MRI, CT...).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "imaging_type_id", nullable = false)
    private ImagingType imagingType;

    // A new machine starts as AVAILABLE.
    public Equipment(String serialNumber, String model,
                     Department department, ImagingType imagingType) {
        this.serialNumber = serialNumber;
        this.model = model;
        this.department = department;
        this.imagingType = imagingType;
        changeStatus(EquipmentStatus.AVAILABLE);
    }

    // No setStatus(): the only way to change the status is this method,
    // which ALWAYS updates the timestamp too (encapsulation).
    public void changeStatus(EquipmentStatus newStatus) {
        this.status = newStatus;
        this.statusUpdatedAt = Instant.now();
    }
}
