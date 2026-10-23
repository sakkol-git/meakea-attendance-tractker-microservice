# Mobile offline command queues and synchronization

> **Note:** An offline command queue allows mobile applications to capture user actions locally during network outages and synchronize them reliably when connectivity is restored.

## Purpose and core concept

Mobile workforce applications frequently operate in environments with intermittent or absent network connectivity (underground basements, remote construction sites, rural agricultural facilities).

An offline command queue stores user intent locally on the mobile device in persistent local storage (such as SQLite or Hive) and dispatches queued commands sequentially when network connectivity returns.

## Problem being solved

Without offline synchronization, mobile workforce applications fail when network connectivity drops:

- Blocked check-in: An employee arrives at work on time, but a spotty cellular connection prevents the mobile app from reaching the server, falsely marking the employee late.
- Lost form inputs: An employee fills out a complex multi-field leave request with attachments, taps submit, and loses all entered data due to a dropped connection.
- Out-of-order execution: Retrying requests haphazardly without a queue can result in a check-out command arriving at the server before its corresponding check-in command.

## Simple mental model

An offline command queue is like a postal mailbox in front of your house.

When the postal truck has already left for the day or the roads are blocked by snow, you still deposit your stamped outgoing letter into the mailbox slot. As soon as the roads are cleared, the postal carrier picks up all waiting letters in the order they were deposited.

## How it works

The offline queue architecture separates user interaction from network I/O:

1. User action: The employee taps "Check In" on their mobile app.
1. Local persistence: The Flutter client generates a client-side UUID, captures the exact local device timestamp, and inserts the command into local SQLite storage with status `PENDING`.
1. Optimistic UI: The mobile UI updates immediately to show "Checked In (Pending Sync)", providing immediate feedback to the employee.
1. Network monitoring: A background connectivity listener monitors device network status.
1. Sequential drain: When internet connectivity is detected, a queue worker reads pending commands in strict chronological order and posts them to the server API with `Idempotency-Key` headers.
1. Server reconciliation: The server validates the command, verifies shift windows against the captured timestamp, commits state, and returns confirmation.
1. Queue pruning: Upon receiving HTTP 200/201, the mobile app marks the local item as `SYNCED` or deletes it from the local queue.

## Why HRFlow needs it

In HRFlow's mobile workforce domain:

- Field attendance: Employees checking in at warehouse docks or remote offices must never be prevented from recording their arrival due to weak Wi-Fi or cellular dead zones.
- Tamper detection: The mobile client records both the device local time and server arrival time, allowing HRFlow's attendance rules to detect artificial clock tampering.
- Guaranteed delivery: Combined with server-side idempotency, the offline queue guarantees that network retries never create duplicate attendance sessions.

## Minimal example

SQLite schema for local mobile queue:

```sql
CREATE TABLE offline_command_queue (
    id TEXT PRIMARY KEY,
    idempotency_key TEXT NOT NULL UNIQUE,
    command_type TEXT NOT NULL,
    payload TEXT NOT NULL,
    created_at TEXT NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    sync_status TEXT NOT NULL DEFAULT 'PENDING'
);
```

## Application-specific implementation

Flutter queue manager in Dart:

```dart
class OfflineQueueService {
  final Database db;
  final ApiClient client;

  OfflineQueueService(this.db, this.client);

  Future<void> enqueueCheckIn(String tenantId, String employeeId) async {
    final command = {
      'id': Uuid().v4(),
      'idempotency_key': Uuid().v4(),
      'command_type': 'ATTENDANCE_CHECK_IN',
      'payload': jsonEncode({
        'tenantId': tenantId,
        'employeeId': employeeId,
        'deviceTimestamp': DateTime.now().toUtc().toIso8601String(),
      }),
      'created_at': DateTime.now().toUtc().toIso8601String(),
      'sync_status': 'PENDING',
    };

    await db.insert('offline_command_queue', command);
    triggerSync();
  }

  Future<void> triggerSync() async {
    final pending = await db.query(
      'offline_command_queue',
      where: 'sync_status = ?',
      whereArgs: ['PENDING'],
      orderBy: 'created_at ASC',
    );

    for (final row in pending) {
      try {
        final response = await client.post(
          '/api/v1/attendance/check-in',
          headers: {'Idempotency-Key': row['idempotency_key'] as String},
          body: row['payload'] as String,
        );

        if (response.statusCode == 200 || response.statusCode == 201) {
          await db.delete('offline_command_queue',
              where: 'id = ?', whereArgs: [row['id']]);
        }
      } catch (e) {
        // Network still unreachable; halt queue processing until next connectivity event
        break;
      }
    }
  }
}
```

## Common mistakes and pitfalls

Avoid these dangerous mobile offline flaws:

> **Pitfall:** Unbounded clock trust. Relying blindly on the mobile device's clock allows users to alter their phone settings backward in time to fake punctuality. The server must compare the device timestamp against server arrival time, flagging large deviations for HR review.

- Processing queue items in parallel: Sending queued commands in parallel can cause a check-out request to arrive at the server before its check-in request. Always drain offline command queues sequentially.
- Missing retry backoff: Retrying failed requests continuously in an infinite loop drains device battery and overloads the server when service is restored. Always apply exponential backoff.
- Discarding validation errors: If the server rejects a queued command with `422 Unprocessable Entity` (e.g., employee was terminated), the queue worker must not retry indefinitely. The failure must be flagged for user resolution.

## Testing and debugging

Validation routine:

- Airplane mode test: Turn on Airplane Mode on the mobile device. Execute an attendance check-in. Confirm that the UI displays a pending sync indicator and the command is stored in local SQLite. Turn off Airplane Mode and verify that the command syncs automatically.
- Server outage simulation: Stop the backend Docker container. Submit mobile requests. Restart the backend container and confirm that the mobile app syncs all queued items in exact chronological order without data loss.
- Idempotency test: Intercept the sync HTTP response and drop it; confirm that the mobile app retries with the same idempotency key and the server creates only one attendance session.

## Trade-offs and alternatives

Architecture comparison:

- Client-side command queue (HRFlow choice): Full offline capability, complete control over synchronization logic, native UI responsiveness; requires custom mobile persistence code.
- Full two-way database replication (CouchDB / PouchDB / Realm): Automatic bidirectional synchronization; highly complex conflict resolution rules and heavy mobile SDK overhead.
- Online-only with retry dialog: Simplest implementation; completely blocks employees when cellular connectivity is poor.

## When not to use it

Do not use offline queuing for:

- Real-time managerial decisions: Emergency evacuations or active security gate unlocking commands must execute in real time or fail immediately.
- High-concurrency inventory reservation: Claiming limited material items offline can lead to unresolvable oversubscription conflicts.

## Related documentation

- [Idempotency primer](IDEMPOTENCY.md)
- [OpenAPI primer](OPENAPI.md)
- [Week 08 web and mobile integration build guide](../08-WEB-AND-MOBILE-INTEGRATION/BUILD-GUIDE.md)
- [Week 04 attendance core build guide](../04-ATTENDANCE-CORE/BUILD-GUIDE.md)
