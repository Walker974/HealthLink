package org.aui.csc.monolith.healthlink.organization.entity;

import jakarta.persistence.*;
import org.aui.csc.monolith.healthlink.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hospitals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hospital extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 250)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    public Hospital(
            String code,
            String name,
            String city,
            String address,
            Region region
    ) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.address = address;
        this.region = region;
    }
}