package com.hrflow.reporting.mapper;

import com.hrflow.reporting.dto.AttendanceFactResponse;
import com.hrflow.reporting.dto.ExportJobResponse;
import com.hrflow.reporting.entity.AttendanceFactProjection;
import com.hrflow.reporting.entity.ExportJob;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ReportingMapper {

    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    @Mapping(target = "version", source = "version", qualifiedByName = "exportApiVersion")
    ExportJobResponse toResponse(ExportJob entity);

    @Named("exportApiVersion")
    default long exportApiVersion(long persistenceRevision) {
        // The existing ExportJob wire contract is one-based; Hibernate revisions start at zero.
        return Math.addExact(persistenceRevision, 1L);
    }

    AttendanceFactResponse toFactResponse(AttendanceFactProjection entity);

    List<AttendanceFactResponse> toFactResponses(List<AttendanceFactProjection> entities);
}
