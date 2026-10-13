package org.aui.csc.monolith.healthlink.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.aui.csc.monolith.healthlink.organization.entity.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {
}