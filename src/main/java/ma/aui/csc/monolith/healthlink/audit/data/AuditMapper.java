package ma.aui.csc.monolith.healthlink.audit.data;

import ma.aui.csc.monolith.healthlink.audit.data.entity.AuditRecord;
import org.mapstruct.*;
import static ma.aui.csc.monolith.healthlink.audit.data.AuditDtos.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuditMapper {
    AuditResponse toResponse(AuditRecord record);
}
