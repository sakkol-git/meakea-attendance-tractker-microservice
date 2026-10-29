package com.hrflow.notification.domain;

import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class CommitAcknowledgment {
    private CommitAcknowledgment() {}
    static void afterCommit(Acknowledgment ack) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { ack.acknowledge(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { ack.acknowledge(); }
        });
    }
}
