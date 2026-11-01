package ma.aui.csc.monolith.healthlink.maintenance.data.repository;

import java.util.Collection;

import ma.aui.csc.monolith.healthlink.maintenance.data.entity.WorkOrder;
import ma.aui.csc.monolith.healthlink.maintenance.data.entity.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    boolean existsByOpenEquipmentId(Long equipmentId);
    Page<WorkOrder> findByEquipment_Id(Long equipmentId, Pageable pageable);

    /** A hospital's repair queue, optionally narrowed to one department. */
    @Query(value = """
            SELECT w FROM WorkOrder w
            JOIN w.equipment e
            JOIN e.department d
            WHERE w.active = true
              AND d.hospital.id = :hospitalId
              AND (:departmentId IS NULL OR d.id = :departmentId)
              AND w.status IN :statuses
            """, countQuery = """
            SELECT COUNT(w) FROM WorkOrder w
            JOIN w.equipment e
            JOIN e.department d
            WHERE w.active = true
              AND d.hospital.id = :hospitalId
              AND (:departmentId IS NULL OR d.id = :departmentId)
              AND w.status IN :statuses
            """)
    Page<WorkOrder> findQueue(
            @Param("hospitalId") Long hospitalId,
            @Param("departmentId") Long departmentId,
            @Param("statuses") Collection<WorkOrderStatus> statuses,
            Pageable pageable
    );
}
