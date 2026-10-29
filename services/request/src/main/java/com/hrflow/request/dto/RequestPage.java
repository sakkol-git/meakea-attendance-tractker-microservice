package com.hrflow.request.dto;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record RequestPage<T>(List<T> items, int page, int size, long total) {
    public static int offset(int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Use page >= 0 and size 1–100 within the supported offset");
        return page * size;
    }
}
