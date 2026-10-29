package com.hrflow.notification.mapper;

import com.hrflow.notification.dto.NotificationPreferencesResponse;
import com.hrflow.notification.entity.NotificationPreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PreferenceMapper {

    @Mapping(target = "emailNotifications", source = "email")
    @Mapping(target = "pushNotifications", source = "inApp")
    NotificationPreferencesResponse toResponse(NotificationPreference preference);
}
