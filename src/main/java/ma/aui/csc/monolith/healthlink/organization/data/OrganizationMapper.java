package ma.aui.csc.monolith.healthlink.organization.data;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Department;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Hospital;
import ma.aui.csc.monolith.healthlink.organization.data.entity.Region;

import static ma.aui.csc.monolith.healthlink.organization.data.OrganizationDtos.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface OrganizationMapper {

    RegionResponse toResponse(Region region);

    @Mapping(target = "regionId", source = "region.id")
    HospitalResponse toResponse(Hospital hospital);

    @Mapping(target = "hospitalId", source = "hospital.id")
    DepartmentResponse toResponse(Department department);
}