package com.hrflow.identityorg.audit;

import java.util.List;

public record AuditPage(List<AuditEntry> items, int page, int size, long total) {}
