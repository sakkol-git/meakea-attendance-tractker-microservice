package com.hrflow.identityorg.organization.dto;

import java.util.UUID;

public record NamedRecord(UUID id, String name, String code, long version) {}
