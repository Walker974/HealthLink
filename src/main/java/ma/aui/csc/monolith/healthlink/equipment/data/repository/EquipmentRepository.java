package ma.aui.csc.monolith.healthlink.equipment.data.repository;

import java.util.List;

import ma.aui.csc.monolith.healthlink.equipment.data.entity.Equipment;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;
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
              AND d.active = true
              AND h.active = true
              AND e.imagingType.active = true
              AND NOT EXISTS (SELECT w.id FROM WorkOrder w WHERE w.openEquipmentId = e.id)
              AND e.imagingType.code = :typeCode
              AND h.city = :city
            """)
    List<Equipment> findByStatusAndTypeInCity(
            @Param("status") EquipmentStatus status,
            @Param("typeCode") String typeCode,
            @Param("city") String city
    );
}
