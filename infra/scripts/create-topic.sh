#!/usr/bin/env bash
set -e
KAFKA="docker exec hrflow-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092"

# compacted reference data
for t in org.employee.v1 org.shift.v1 org.holiday.v1; do
  $KAFKA --create --if-not-exists --topic $t --partitions 6 --replication-factor 1 \
    --config cleanup.policy=compact
done

# normal event topics
for t in attendance.evidence.v1 attendance.session.v1 attendance.daily-status.v1 \
         attendance.leave-conflict.v1 leave.request.v1 request.workflow.v1 time.shift-cutoff.v1; do
  $KAFKA --create --if-not-exists --topic $t --partitions 6 --replication-factor 1
done
$KAFKA --list