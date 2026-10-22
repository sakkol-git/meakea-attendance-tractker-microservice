# Week 07 build guide

> **Navigation:** [Previous: Week 06](../06-LEAVE-WORKFLOW/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 08](../08-WEB-AND-MOBILE-INTEGRATION/README.md)

## Day plan

| Day | Work | New concepts | Time cap per person |
| --- | --- | --- | --- |
| Monday | Stone 1, three small steps | First two linked primers only | 4 h |
| Tuesday | Stone 2, three small steps | Next two if needed | 4 h |
| Wednesday | Stone 3, three small steps | At most two | 4 h |
| Thursday | Stone 4, review and recovery only | None | 1.8 h |
| Friday | Stone 5, gate and redeploy | None | 3 h |
| Weekend | Two rest days | None | 0 h |

Totals stay within 16.8 h per contributor; unused time is buffer, not permission for more scope. Each code example is a proposed minimal pattern/contract; inspect first and adapt names to verified files. Run Maven from repository root; run Flutter tests from the confirmed mobile root. A/B step pairing may use reserved buffer, never exceed day cap.

## Stone 1 — Drafts and bounded forms

**You will have:** Two real request types can be submitted and errors render without raw JSON dumps.

### W07-S01 — Create request and advisory catalogue (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Adapter primer](../primers/ADAPTER.md)

**Before you start:**

- [ ] Week 06 critical gate is green or its carried failure replaces this action.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Add request_type/material/request/line/step/comment migrations; stock advisory; SPECIAL leave stays in leave and general special requests use OTHER.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Draft and line constraints pass; no implied inventory reservation.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-07): create request and advisory catalogue`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W07-S02 — Validate and submit a snapshot (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Adapter primer](../primers/ADAPTER.md)

**Before you start:**

- [ ] W07-S01 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Typed bounded JSON forms, draft-only edits and submitted chain snapshot; resolve real assigned approver, reject missing/self-only chain.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Draft edit succeeds; submitted edit fails; correct approver receives work.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-07): validate and submit a snapshot`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W07-S03 — B renders draft and request detail (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Adapter primer](../primers/ADAPTER.md)

**Before you start:**

- [ ] W07-S02 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use mock schemas for material/other forms and quantity; show participants/comments/status; no arbitrary script schemas.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Two real request types can be submitted and errors render without raw JSON dumps.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-07): b renders draft and request detail`.
- [ ] Tagged `w07-s03-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 2 — Safe decisions and fulfillment

**You will have:** Wrong role cannot trigger a hidden button through direct HTTP.

### W07-S04 — Approve exactly the current step (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Attachment Security primer](../primers/ATTACHMENT-SECURITY.md)

**Before you start:**

- [ ] W07-S03 committed with its check passing; tag `w07-s03-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Lock request/current step/version; allow assigned independent approver only; append action audit and advance chain.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Parallel decisions have one winner; unrelated manager and requester cannot approve.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-07): approve exactly the current step`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W07-S05 — Reject cancel and fulfill explicitly (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Attachment Security primer](../primers/ATTACHMENT-SECURITY.md)

**Before you start:**

- [ ] W07-S04 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Reject requires note; requester cancels before final decision; HR fulfills approved materials with handover reason; no authoritative stock promise.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Terminal transitions and duplicate fulfillment tests pass.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-07): reject cancel and fulfill explicitly`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W07-S06 — B verifies manager and requester views (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Attachment Security primer](../primers/ATTACHMENT-SECURITY.md)

**Before you start:**

- [ ] W07-S05 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Exercise submit/approve/reject/cancel/fulfill through mobile and React; refresh after 412; show who is authorized now.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Wrong role cannot trigger a hidden button through direct HTTP.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-07): b verifies manager and requester views`.
- [ ] Tagged `w07-s06-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 3 — Attachment adapter only after pain

**You will have:** Tests unchanged; swapping storage does not change permission outcomes or expose object keys.

### W07-S07 — Build plain private file storage (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Attachment Security primer](../primers/ATTACHMENT-SECURITY.md)

**Before you start:**

- [ ] W07-S06 committed with its check passing; tag `w07-s06-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Implement plain private disk upload/download with generated keys, size/type/checksum and participant authorization; include file directory in backup.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Valid attachment downloads only for participant; traversal and cross-tenant requests fail.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-07): build plain private file storage`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W07-S08 — Prove storage isolation pain (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Attachment Security primer](../primers/ATTACHMENT-SECURITY.md)

**Before you start:**

- [ ] W07-S07 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Show tests requiring repeated filesystem setup and failure injection; answer admission test for production disk and isolated test implementation.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Same storage behavior suite demonstrates write/read failure and no leaked public paths.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-07): prove storage isolation pain`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W07-S09 — Refactor to a storage adapter if admitted (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Attachment Security primer](../primers/ATTACHMENT-SECURITY.md)

**Before you start:**

- [ ] W07-S08 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Add small StoragePort with disk and test adapters; keep authorization in module service, never in provider; preserve tests and backup manifest.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Tests unchanged; swapping storage does not change permission outcomes or expose object keys.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-07): refactor to a storage adapter if admitted`.
- [ ] Tagged `w07-s09-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Continue

[Part two: buffer and Friday](BUILD-GUIDE-PART-2.md).
