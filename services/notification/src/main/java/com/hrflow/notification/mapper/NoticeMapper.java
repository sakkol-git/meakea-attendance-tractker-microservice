package com.hrflow.notification.mapper;

import com.hrflow.notification.dto.NoticeResponse;
import com.hrflow.notification.entity.NotificationRecord;
import java.time.Instant;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface NoticeMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "to", constant = "")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToString")
    NoticeResponse toResponse(NotificationRecord record);

    @Named("uuidToString")
    default String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }

    @Named("instantToString")
    default String instantToString(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
