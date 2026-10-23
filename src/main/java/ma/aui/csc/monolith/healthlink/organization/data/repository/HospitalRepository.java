package ma.aui.csc.monolith.healthlink.organization.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Hospital;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    Page<Hospital> findByRegion_Id(Long regionId, Pageable pageable);
}