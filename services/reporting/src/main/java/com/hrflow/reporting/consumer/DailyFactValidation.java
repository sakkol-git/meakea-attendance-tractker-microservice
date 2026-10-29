package com.hrflow.reporting.consumer;

import com.hrflow.contracts.attendance.AttendanceDailyFact;
import com.hrflow.contracts.attendance.AttendanceDailyWire;
import org.apache.kafka.clients.consumer.ConsumerRecord;

final class DailyFactValidation {
    private DailyFactValidation() {}
    static AttendanceDailyWire.Identity validate(ConsumerRecord<String, AttendanceDailyFact> record) {
        if (!"attendance.daily-status.v1".equals(record.topic()) || record.partition() < 0 || record.offset() < 0)
            throw new IllegalArgumentException("Invalid daily fact transport");
        return AttendanceDailyWire.validate(record.key(), record.value());
    }
}
