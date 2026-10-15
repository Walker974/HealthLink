package org.aui.csc.monolith.healthlink.equipment.repository;

import java.util.List;

import org.aui.csc.monolith.healthlink.equipment.entity.Equipment;
import org.aui.csc.monolith.healthlink.equipment.entity.EquipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByStatus(EquipmentStatus status);
    List<Equipment> findByDepartment_Id(Long departmentId);
    @Query("""
            SELECT e FROM Equipment e
            JOIN e.department d
            JOIN d.hospital h
            WHERE e.status = :status
              AND e.active = true
              AND e.imagingType.code = :typeCode
              AND h.city = :city
            """)
    List<Equipment> findByStatusAndTypeInCity(
            @Param("status") EquipmentStatus status,
            @Param("typeCode") String typeCode,
            @Param("city") String city
    );
}
