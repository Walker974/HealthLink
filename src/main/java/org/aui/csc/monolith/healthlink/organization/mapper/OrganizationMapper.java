package org.aui.csc.monolith.healthlink.organization.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.aui.csc.monolith.healthlink.organization.entity.Department;
import org.aui.csc.monolith.healthlink.organization.entity.Hospital;
import org.aui.csc.monolith.healthlink.organization.entity.Region;

import static org.aui.csc.monolith.healthlink.organization.dto.OrganizationDtos.*;

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