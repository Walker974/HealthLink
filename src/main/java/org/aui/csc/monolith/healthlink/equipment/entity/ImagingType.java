package org.aui.csc.monolith.healthlink.equipment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.aui.csc.monolith.healthlink.common.entity.BaseEntity;

/**
 * A kind of imaging service: MRI, CT, X-ray, ultrasound...
 *
 * It is an ENTITY (a table), not an enum, on purpose:
 * the Ministry can add a new imaging type as DATA,
 * without changing the Java code (extensibility NFR).
 */
@Entity
@Table(name = "imaging_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImagingType extends BaseEntity {   // inherits id, active, audit fields

    // Short unique code used in URLs and filters, e.g. "MRI", "CT".
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    // Human-readable name, e.g. "Magnetic Resonance Imaging".
    @Column(nullable = false, length = 120)
    private String name;

    public ImagingType(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
