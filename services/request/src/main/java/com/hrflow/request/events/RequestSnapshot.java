package com.hrflow.request.events;

import java.time.Instant;
import java.util.List;

/** Minimal downstream facts; private text and account audit stay with their owner. */
public record RequestSnapshot(String employeeId, String requestType, String state, Instant createdAt, List<Step> steps) {
    public record Step(int order, String role, String approverId, String state) {}
}
