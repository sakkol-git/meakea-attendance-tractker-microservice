package com.hrflow.request.service;

import java.util.Set;

/** Verified account provenance and its employee relationship are distinct identities. */
public record RequestCaller(String account, String employee, Set<String> roles) {
    public RequestCaller {
        if (account == null || account.isBlank() || employee == null || employee.isBlank()) {
            throw new IllegalArgumentException("Account and employee identities are required");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean owns(String requester) {
        return requester.equals(employee) || requester.equals(account);
    }
}
