package ma.aui.csc.monolith.healthlink.staff.data.repository;

import java.util.Optional;

import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {

    @EntityGraph(attributePaths = {"hospital", "department", "department.hospital"})
    Optional<StaffMember> findByKeycloakSubjectAndActiveTrue(String keycloakSubject);
}
