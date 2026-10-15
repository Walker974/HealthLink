package org.aui.csc.monolith.healthlink.equipment.repository;

import java.util.Optional;

import org.aui.csc.monolith.healthlink.equipment.entity.ImagingType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagingTypeRepository extends JpaRepository<ImagingType, Long> {
    Optional<ImagingType> findByCode(String code);
}
