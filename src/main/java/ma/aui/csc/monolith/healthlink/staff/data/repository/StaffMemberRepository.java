package ma.aui.csc.monolith.healthlink.staff.data.repository;

import java.util.Optional;
import jakarta.persistence.QueryHint;

import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {

    @EntityGraph(attributePaths = {"hospital", "department", "department.hospital"})
    @QueryHints(@QueryHint(name = "org.hibernate.flushMode", value = "COMMIT"))
    Optional<StaffMember> findByKeycloakSubjectAndActiveTrue(String keycloakSubject);
}
