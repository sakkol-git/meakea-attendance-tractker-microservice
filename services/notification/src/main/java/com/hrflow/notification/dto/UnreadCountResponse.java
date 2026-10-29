package com.hrflow.notification.dto;

public record UnreadCountResponse(
        long count,
        long unreadCount
) {}
