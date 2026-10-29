package com.hrflow.identityorg.organization.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkSiteRecordV2(UUID id, String name, BigDecimal latitude, BigDecimal longitude,
        int radiusM, String timezone, long version) {}
