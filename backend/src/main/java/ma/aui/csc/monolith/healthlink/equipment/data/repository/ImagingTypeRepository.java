package ma.aui.csc.monolith.healthlink.equipment.data.repository;

import java.util.Optional;

import ma.aui.csc.monolith.healthlink.equipment.data.entity.ImagingType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagingTypeRepository extends JpaRepository<ImagingType, Long> {
    Optional<ImagingType> findByCode(String code);
}
