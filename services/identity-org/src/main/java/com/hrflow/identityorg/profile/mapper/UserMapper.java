package com.hrflow.identityorg.profile.mapper;

import com.hrflow.identityorg.auth.service.record.AuthRoles;
import com.hrflow.identityorg.profile.dto.UserProfileResponse;
import com.hrflow.identityorg.profile.entity.User;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "userId", expression = "java(user.getId().toString())")
    @Mapping(target = "employeeId", expression = "java(user.getEmployeeId().toString())")
    @Mapping(target = "tenantId", expression = "java(user.getTenantId().toString())")
    @Mapping(target = "name", source = "displayName")
    @Mapping(target = "locale", constant = "en")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "splitRoles")
    UserProfileResponse toProfileResponse(User user);

    @Named("splitRoles")
    default List<String> splitRoles(String roles) {
        return AuthRoles.parse(roles);
    }
}
