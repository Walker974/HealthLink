package ma.aui.csc.monolith.healthlink.maintenance.data;

import ma.aui.csc.monolith.healthlink.maintenance.data.entity.WorkOrder;
import org.mapstruct.*;
import static ma.aui.csc.monolith.healthlink.maintenance.data.WorkOrderDtos.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WorkOrderMapper {
    @Mapping(target = "equipmentId", source = "equipment.id")
    @Mapping(target = "equipmentVersion", source = "equipment.version")
    @Mapping(target = "equipmentStatus", source = "equipment.status")
    @Mapping(target = "hospitalId", source = "equipment.department.hospital.id")
    @Mapping(target = "departmentId", source = "equipment.department.id")
    @Mapping(target = "reporterId", source = "reporter.id")
    @Mapping(target = "repairerId", source = "repairer.id")
    @Mapping(target = "verifierId", source = "verifier.id")
    WorkOrderResponse toResponse(WorkOrder order);
}
