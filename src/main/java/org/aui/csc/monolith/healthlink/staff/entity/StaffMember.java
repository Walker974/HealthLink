package org.aui.csc.monolith.healthlink.staff.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.aui.csc.monolith.healthlink.common.entity.BaseEntity;
import org.aui.csc.monolith.healthlink.organization.entity.Hospital;
import org.aui.csc.monolith.healthlink.organization.entity.Department;

/**
 * A member of staff who can log in and change data.
 *
 * Privacy (Law 09-08, data minimization): we store only what the
 * platform needs. No password (Keycloak will handle login),
 * no national ID, no phone, no home address.
 */
@Entity
@Table(name = "staff_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffMember extends BaseEntity {   // inherits id + active

    // Nullable only to preserve legacy rows. Unlinked accounts cannot access staff operations.
    @Column(unique = true, length = 255)
    private String keycloakSubject;

    @Column(nullable = false, length = 120)
    private String fullName;

    // Unique: one account per email. Will match the Keycloak login.
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    // EnumType.STRING stores the NAME ("HOSPITAL_MANAGER") in the column.
    // The default (ORDINAL) would store a position number (0, 1, 2...),
    // which silently breaks if someone reorders the enum.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StaffRole role;

    // Many staff members -> one hospital.
    // Optional (nullable): Ministry officers and platform admins
    // do not belong to a single hospital.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    public StaffMember(String fullName, String email, StaffRole role, Hospital hospital) {
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.hospital = hospital;
    }

    public StaffMember(String keycloakSubject, String fullName, String email,
                       StaffRole role, Hospital hospital, Department department) {
        this(fullName, email, role, hospital);
        this.keycloakSubject = keycloakSubject;
        this.department = department;
    }
}
