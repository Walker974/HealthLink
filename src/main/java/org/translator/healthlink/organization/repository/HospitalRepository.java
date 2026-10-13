package org.translator.healthlink.organization.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.translator.healthlink.organization.entity.Hospital;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    Page<Hospital> findByRegion_Id(Long regionId, Pageable pageable);
}