package com.hrflow.notification.dto;

public record UpdatePreferencesRequest(
        Boolean email,
        Boolean emailNotifications,
        Boolean inApp,
        Boolean pushNotifications,
        Boolean reminders,
        String digestFrequency
) {
    public boolean resolvedEmail(boolean current) {
        if (email != null) return email;
        if (emailNotifications != null) return emailNotifications;
        return current;
    }

    public boolean resolvedInApp(boolean current) {
        if (inApp != null) return inApp;
        if (pushNotifications != null) return pushNotifications;
        return current;
    }

    public boolean resolvedReminders(boolean current) {
        if (reminders != null) return reminders;
        return current;
    }

    public String resolvedDigest(String current) {
        if (digestFrequency != null && !digestFrequency.isBlank()) return digestFrequency;
        return current;
    }
}
