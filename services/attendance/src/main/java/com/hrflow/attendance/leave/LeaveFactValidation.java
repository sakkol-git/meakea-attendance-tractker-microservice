package com.hrflow.attendance.leave;

import com.hrflow.contracts.leave.LeaveRequestFact;
import com.hrflow.contracts.leave.LeaveRequestWire;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;

final class LeaveFactValidation {
    private LeaveFactValidation() {}
    static Identity validate(ConsumerRecord<String, LeaveRequestFact> record) {
        if (!"leave.request.v1".equals(record.topic()) || record.partition() < 0 || record.offset() < 0)
            throw new IllegalArgumentException("Invalid leave record transport");
        var id = LeaveRequestWire.validate(record.key(), record.value());
        return new Identity(id.tenant(), id.leave(), id.employee(), id.event());
    }
    record Identity(UUID tenant, UUID leave, UUID employee, UUID event) {}
}
