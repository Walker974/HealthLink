package ma.aui.csc.monolith.healthlink.equipment.data;

import ma.aui.csc.monolith.healthlink.equipment.data.ImagingTypeDtos.ImagingTypeResponse;
import ma.aui.csc.monolith.healthlink.equipment.data.entity.ImagingType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ImagingTypeMapper {
    ImagingTypeResponse toResponse(ImagingType imagingType);
}
