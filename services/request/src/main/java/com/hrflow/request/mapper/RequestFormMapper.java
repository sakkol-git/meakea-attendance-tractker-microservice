package com.hrflow.request.mapper;

import com.hrflow.request.dto.RequestForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class RequestFormMapper {
    private final JsonMapper json;

    public RequestForm form(String raw) {
        if (raw == null) return new RequestForm(null, null, null, null);
        var values = json.readTree(raw);
        return new RequestForm(text(values, "startDate"), text(values, "endDate"), text(values, "fromTime"), text(values, "toTime"));
    }
    private String text(tools.jackson.databind.JsonNode values, String key) {
        var value = values.get(key);
        return value == null || value.isNull() ? null : value.asText();
    }
}
