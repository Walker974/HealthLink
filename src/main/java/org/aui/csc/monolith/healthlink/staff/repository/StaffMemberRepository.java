package org.aui.csc.monolith.healthlink.staff.repository;

import java.util.Optional;

import org.aui.csc.monolith.healthlink.staff.entity.StaffMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {

    @EntityGraph(attributePaths = {"hospital", "department", "department.hospital"})
    Optional<StaffMember> findByKeycloakSubjectAndActiveTrue(String keycloakSubject);
}
