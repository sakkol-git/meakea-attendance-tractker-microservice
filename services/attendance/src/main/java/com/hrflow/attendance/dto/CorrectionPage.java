package com.hrflow.attendance.dto;

import java.util.List;

public record CorrectionPage(List<CorrectionResponse> items, int page, int size, long total) {}
