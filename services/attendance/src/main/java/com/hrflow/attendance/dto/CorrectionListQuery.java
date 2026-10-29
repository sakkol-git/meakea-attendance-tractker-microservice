package com.hrflow.attendance.dto;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record CorrectionListQuery(Integer page, Integer size, String search, String status, String sort, Integer minAgeDays) {
    public int pageNumber() { return page == null ? 0 : page; }
    public int pageSize() { return size == null ? 25 : size; }
    public int offset() {
        if (pageNumber() < 0 || pageSize() < 1 || pageSize() > 100 || (long) pageNumber() * pageSize() > Integer.MAX_VALUE
                || minAgeDays != null && minAgeDays < 0
                || sort != null && !sort.isBlank() && !sort.equals("oldest") && !sort.equals("newest"))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid correction page or filter");
        return pageNumber() * pageSize();
    }
}
