package com.hrflow.notification.dto;

import java.util.List;

public record NotificationPageResponse(
        List<NoticeResponse> items,
        int page,
        int size,
        long total
) {}
