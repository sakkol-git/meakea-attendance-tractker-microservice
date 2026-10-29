package com.hrflow.leave.service;

import java.util.Collection;

/** Separate verified account and employee identities; neither comes from command JSON. */
public record LeaveCaller(String actor, String employee, Collection<String> roles) {
    public boolean hrAdmin() { return has("HR_ADMIN"); }
    public boolean manager() { return has("MANAGER"); }
    private boolean has(String role) { return roles != null && roles.stream().anyMatch(value -> value.equals(role) || value.equals("ROLE_" + role)); }
}
