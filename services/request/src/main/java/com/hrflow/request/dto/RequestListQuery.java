package com.hrflow.request.dto;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record RequestListQuery(Integer page, Integer size, String search, String status, String sort, Integer minAgeDays) {
    public int pageNumber() { return page == null ? 0 : page; }
    public int pageSize() { return size == null ? 25 : size; }
    public int offset() {
        if (minAgeDays != null && minAgeDays < 0
                || sort != null && !sort.isBlank() && !sort.equals("oldest") && !sort.equals("newest"))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid request list filter");
        return RequestPage.offset(pageNumber(), pageSize());
    }
    public boolean newest(boolean inbox) { return sort == null || sort.isBlank() ? !inbox : sort.equals("newest"); }
}
