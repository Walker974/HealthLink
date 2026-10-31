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
    @Mapping(target = "hospitalId", source = "department.hospital.id")
    @Mapping(target = "hospitalName", source = "department.hospital.name")
    @Mapping(target = "city", source = "department.hospital.city")
    @Mapping(target = "address", source = "department.hospital.address")
    EquipmentResponse toResponse(Equipment equipment);
}
