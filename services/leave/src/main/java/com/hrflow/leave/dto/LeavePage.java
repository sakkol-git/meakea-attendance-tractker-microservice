package com.hrflow.leave.dto;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record LeavePage<T>(List<T> items, int page, int size, long total) {
    public static PageRequest request(int page, int size, String... order) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid page or size");
        }
        return PageRequest.of(page, size, Sort.by(order));
    }

    public static <T> LeavePage<T> from(Page<T> source) {
        return new LeavePage<>(source.getContent(), source.getNumber(), source.getSize(), source.getTotalElements());
    }
}
