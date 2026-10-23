package ma.aui.csc.monolith.healthlink.equipment.data;

import ma.aui.csc.monolith.healthlink.equipment.data.EquipmentDtos.EquipmentResponse;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.Equipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface EquipmentMapper {

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "imagingTypeCode", source = "imagingType.code")
    EquipmentResponse toResponse(Equipment equipment);
}
