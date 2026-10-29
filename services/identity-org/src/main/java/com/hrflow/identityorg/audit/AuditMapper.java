package com.hrflow.identityorg.audit;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditMapper {
    @Mapping(target = "actor", source = "actorId")
    @Mapping(target = "changes", source = "diff")
    AuditEntry toEntry(IdentityAudit source);
}
