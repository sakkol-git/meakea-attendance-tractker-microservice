package com.hrflow.notification.dto;

public record NoticeResponse(
        String id,
        String title,
        boolean read,
        String detail,
        String body,
        String createdAt,
        String to,
        String recordId,
        String kind
) {}
