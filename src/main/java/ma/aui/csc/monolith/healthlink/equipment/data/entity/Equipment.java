package ma.aui.csc.monolith.healthlink.equipment.data.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ma.aui.csc.monolith.healthlink.common.entity.BaseEntity;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Department;

/**
 * One physical machine (e.g. the MRI scanner of the radiology department
 * of CHU Hassan II) and its current operational status.
 * This is the heart of HealthLink: citizens search for AVAILABLE equipment.
 */
@Entity
@Table(name = "equipment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Equipment extends BaseEntity {

    @Column(nullable = false, unique = true, length = 60)
    private String serialNumber;

    @Column(nullable = false, length = 120)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EquipmentStatus status;

    @Column(nullable = false)
    private Instant statusUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "imaging_type_id", nullable = false)
    private ImagingType imagingType;

    public Equipment(String serialNumber, String model,
                     Department department, ImagingType imagingType) {
        this.serialNumber = serialNumber;
        this.model = model;
        this.department = department;
        this.imagingType = imagingType;
        changeStatus(EquipmentStatus.AVAILABLE);
    }

    public void changeStatus(EquipmentStatus newStatus) {
        this.status = newStatus;
        this.statusUpdatedAt = Instant.now();
    }
}
