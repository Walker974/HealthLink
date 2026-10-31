package ma.aui.csc.monolith.healthlink.staff.data;

import ma.aui.csc.monolith.healthlink.staff.data.StaffDtos.StaffResponse;
import ma.aui.csc.monolith.healthlink.staff.data.entity.StaffMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface StaffMapper {

    @Mapping(target = "hospitalId", source = "hospital.id")
    @Mapping(target = "departmentId", source = "department.id")
    StaffResponse toResponse(StaffMember staff);
}
