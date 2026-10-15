package org.aui.csc.monolith.healthlink.equipment.repository;

import java.util.Optional;

import org.aui.csc.monolith.healthlink.equipment.entity.ImagingType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for ImagingType.
 */
public interface ImagingTypeRepository extends JpaRepository<ImagingType, Long> {

    // Derived query: SELECT t FROM ImagingType t WHERE t.code = ?1
    // Optional = "maybe found, maybe not" (no null).
    Optional<ImagingType> findByCode(String code);
}
