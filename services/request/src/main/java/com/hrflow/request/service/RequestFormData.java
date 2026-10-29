package com.hrflow.request.service;

import com.hrflow.request.dto.RequestFormInput;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** Form data records the request; it does not alter leave or attendance without a separate accepted workflow. */
@Component
@RequiredArgsConstructor
public class RequestFormData {
    private final JsonMapper json;

    public String merge(String existing, RequestFormInput input) {
        var values = new LinkedHashMap<String, Object>();
        read(values, existing);
        read(values, input.getFormData());
        put(values, "startDate", input.getStartDate()); put(values, "endDate", input.getEndDate());
        put(values, "fromTime", input.getFromTime()); put(values, "toTime", input.getToTime());
        try {
            if (values.containsKey("startDate") || values.containsKey("endDate")) {
                var first = LocalDate.parse(text(values, "startDate"));
                var last = LocalDate.parse(text(values, "endDate"));
                if (last.isBefore(first)) throw invalid();
            }
            if (values.containsKey("fromTime") || values.containsKey("toTime")) {
                var first = LocalTime.parse(text(values, "fromTime"));
                var last = LocalTime.parse(text(values, "toTime"));
                if (!last.isAfter(first)) throw invalid();
            }
        } catch (java.time.DateTimeException error) { throw invalid(); }
        return values.isEmpty() ? null : json.writeValueAsString(values);
    }

    private void read(Map<String, Object> values, String raw) {
        if (raw == null) return;
        try {
            var node = json.readTree(raw);
            if (raw.length() > 32768 || !node.isObject()) throw invalid();
            values.putAll(json.convertValue(node, Map.class));
        } catch (tools.jackson.core.JacksonException error) { throw invalid(); }
    }
    private void put(Map<String, Object> values, String key, String value) { if (value != null) values.put(key, value); }
    private String text(Map<String, Object> values, String key) {
        if (!(values.get(key) instanceof String value) || value.isBlank()) throw invalid();
        return value;
    }
    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Use a valid request JSON object and ordered date/time ranges"); }
}
