package ma.aui.csc.monolith.healthlink.maintenance.data.repository;

import ma.aui.csc.monolith.healthlink.maintenance.data.entity.WorkOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    boolean existsByOpenEquipmentId(Long equipmentId);
    Page<WorkOrder> findByEquipment_Id(Long equipmentId, Pageable pageable);
}
