package ma.aui.csc.monolith.healthlink.equipment.data.repository;

import java.util.List;

import ma.aui.csc.monolith.healthlink.equipment.data.entity.Equipment;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.EquipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Staff inventory: active equipment of one hospital, optionally narrowed to a department or a status. */
    @Query(value = """
            SELECT e FROM Equipment e
            JOIN e.department d
            WHERE e.active = true
              AND d.hospital.id = :hospitalId
              AND (:departmentId IS NULL OR d.id = :departmentId)
              AND (:status IS NULL OR e.status = :status)
            """, countQuery = """
            SELECT COUNT(e) FROM Equipment e
            JOIN e.department d
            WHERE e.active = true
              AND d.hospital.id = :hospitalId
              AND (:departmentId IS NULL OR d.id = :departmentId)
              AND (:status IS NULL OR e.status = :status)
            """)
    Page<Equipment> findInventory(
            @Param("hospitalId") Long hospitalId,
            @Param("departmentId") Long departmentId,
            @Param("status") EquipmentStatus status,
            Pageable pageable
    );
}
