package com.hrflow.attendance.mapper;

import com.hrflow.attendance.entity.OriginalAttendanceSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class OriginalSessionJson {
    private final JsonMapper json;
    public List<OriginalAttendanceSession> read(String value) {
        return value == null ? null : json.readValue(value, new TypeReference<List<OriginalAttendanceSession>>() {});
    }
}
