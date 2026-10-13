package org.translator.healthlink.organization.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "departments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_departments_hospital_code",
                columnNames = {"hospital_id", "code"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    public Department(String code, String name, Hospital hospital) {
        this.code = code;
        this.name = name;
        this.hospital = hospital;
    }
}