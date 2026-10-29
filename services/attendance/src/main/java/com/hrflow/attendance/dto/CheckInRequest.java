package com.hrflow.attendance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckInRequest(
        Double latitude,
        Double longitude,
        Double accuracyMeters,
        String deviceModel
) {}
