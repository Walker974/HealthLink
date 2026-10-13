package org.translator.healthlink.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.translator.healthlink.organization.entity.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {
}