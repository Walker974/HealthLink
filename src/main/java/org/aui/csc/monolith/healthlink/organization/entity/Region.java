package org.aui.csc.monolith.healthlink.organization.entity;

import jakarta.persistence.*;
import org.aui.csc.monolith.healthlink.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "regions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    public Region(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
