# Test strategy

## Levels and minimum scenarios

| Level | Location after creation | What it proves | Fast command |
| --- | --- | --- | --- |
| Domain | App src/test/java mirrored internal.domain/service | Time/grace, transitions, exact arithmetic | Maven selected method |
| Repository | Same tree, integration tests | PostgreSQL 16 migrations, tenant keys, locks, RLS | Selected integration class |
| Contract | OpenAPI fixtures and HTTP integration tests | Request/response/error compatibility | Contract check plus route test |
| Architecture | App test architecture package | Cycles/foreign internals fail | Selected architecture class |
| Client | React/Flutter feature tests | Loading/errors, refresh, queue replay | npm/Flutter selected file |
| End to end | Proposed acceptance scripts plus isolated fixtures | One employee/manager journey and deployment smoke | Weekly scenario runner |

All locations are proposed: inspect current tree first; none of the monolith tests exists in the audit. Use names ending Test for fast Surefire execution; integration tests also use Test until a separate Failsafe convention is explicitly configured, avoiding silent unexecuted IT files.

## One red test first

1. Read the target behavior and choose one fixed input/output or denied operation.
1. Find the nearest existing test; record exact path and test method. If absent, create one mirrored test file.
1. Add an assertion that fails on the missing behavior. Run only that method and verify failure reason is semantic.
1. Change the smallest production file, rerun the method, then its relevant negative test and full required suite.
1. Commit each small step. Red commits remain on a work branch and are never green-tagged/deployed; protected branch only accepts passing CI.

```bash
mvn -f services/hrflow-app/pom.xml -Dtest=RECORDED_CLASS#RECORDED_METHOD test
```

RECORDED_CLASS/RECORDED_METHOD are replacement tokens taken from inspection, not invented existing classes. Proposed new golden-slice test names are documented in that slice before creation.

## Critical coverage target

JaCoCo checks only actual auth, attendance and leave domain/service packages: at least 80 percent lines and 70 percent branches per selected critical area. Do not lower the number by excluding difficult decision code. DTO/generated/bootstrap code is not counted; migration/role/lock correctness is separately verified through integration tests. Coverage never proves authorization or concurrency by itself.

## Debugging container failures

Check target Java 21, docker info, disk headroom, image architecture and image pull availability. Missing Docker or dependency artifact is **FAIL** environment, not a failed business assertion. Inspect bounded container logs without dumping credentials. Use disposable unique container/DB names; never clear a production volume. Re-run selected test with fixed clock/data seed, then full suite after the root cause is fixed.

## Shared endpoint matrix

Every implemented OpenAPI route registers happy input, invalid input, anonymous/invalid token, wrong role/resource, tenant-B ID, retry key/body conflict, applicable race, DB integrity, failure recovery and regression. A genuinely inapplicable case has a written reason; no generic “passed” echo counts. Test count and route registry must agree so a new route cannot silently skip tenant coverage.

See [golden slice](GOLDEN-SLICE.md), [daily commands](DAILY-COMMANDS.md) and [standards](ENGINEERING-STANDARDS.md).
