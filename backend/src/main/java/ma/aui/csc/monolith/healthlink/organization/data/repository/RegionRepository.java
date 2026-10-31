package ma.aui.csc.monolith.healthlink.organization.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {
}