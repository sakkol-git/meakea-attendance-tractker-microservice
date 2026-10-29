package com.hrflow.notification.dto;

public record NotificationPreferencesResponse(
        boolean email,
        boolean emailNotifications,
        boolean inApp,
        boolean pushNotifications,
        boolean reminders,
        String digestFrequency,
        long version
) {}
