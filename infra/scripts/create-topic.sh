#!/usr/bin/env bash
set -euo pipefail

topic() {
  docker exec hrflow-kafka kafka-topics --bootstrap-server kafka:29092 "$@"
}

# compacted reference data
for t in org.employee.v1 org.shift.v1 org.holiday.v1; do
  topic --create --if-not-exists --topic "$t" --partitions 6 --replication-factor 1 \
    --config cleanup.policy=compact
done

# normal event topics
for t in attendance.evidence.v1 attendance.session.v1 attendance.correction.v1 attendance.daily-status.v1 \
         attendance.leave-conflict.v1 leave.request.v1 request.workflow.v1 time.shift-cutoff.v1; do
  topic --create --if-not-exists --topic "$t" --partitions 6 --replication-factor 1
done
# Delayed retry partitions match their six-partition source; private DLQ retains 14 days.
for source in org.employee.v1 org.shift.v1 org.holiday.v1 attendance.session.v1 attendance.correction.v1 attendance.daily-status.v1 leave.request.v1 request.workflow.v1 time.shift-cutoff.v1; do
  for attempt in 0 1 2; do
    topic --create --if-not-exists --topic "$source.retry-$attempt" --partitions 6 --replication-factor 1 --config retention.ms=259200000
  done
  topic --create --if-not-exists --topic "$source.dlq" --partitions 1 --replication-factor 1 --config retention.ms=1209600000
done
topic --list
