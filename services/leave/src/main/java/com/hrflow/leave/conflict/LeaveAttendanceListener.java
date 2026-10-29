package com.hrflow.leave.conflict;

import com.hrflow.contracts.attendance.AttendanceDailyFact;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.leave.attendance-consumer.enabled", havingValue = "true")
public class LeaveAttendanceListener {
    private final LeaveAttendanceReview review;
    @KafkaListener(topics = "attendance.daily-status.v1", groupId = "hrflow-leave-attendance-v1")
    public void receive(ConsumerRecord<String, AttendanceDailyFact> record, Acknowledgment acknowledgment) {
        review.apply(record.key(), record.value());
        if (!TransactionSynchronizationManager.isSynchronizationActive()) acknowledgment.acknowledge();
        else TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { acknowledgment.acknowledge(); }
        });
    }
}
