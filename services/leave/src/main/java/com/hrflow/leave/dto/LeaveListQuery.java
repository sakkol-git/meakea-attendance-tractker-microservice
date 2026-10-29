package com.hrflow.leave.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record LeaveListQuery(Integer page, Integer size, String search, String status, String sort, Integer minAgeDays) {
    public PageRequest request() {
        if (minAgeDays != null && minAgeDays < 0
                || sort != null && !sort.isBlank() && !sort.equals("oldest") && !sort.equals("newest")) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid leave list filter");
        }
        return LeavePage.request(page == null ? 0 : page, size == null ? 25 : size, "createdAt", "id");
    }

    public boolean newest(boolean pending) {
        return sort == null || sort.isBlank() ? !pending : sort.equals("newest");
    }
}
