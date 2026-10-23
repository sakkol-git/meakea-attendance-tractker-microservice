# Template Method pattern and workflow skeletons

> **Note:** The Template Method pattern defines the invariant skeleton of an algorithm in an abstract base class, deferring some concrete steps to subclasses without altering the algorithm's overarching structure.

## Purpose and core concept

The Template Method pattern establishes a fixed sequence of execution steps in a base class template method, while allowing subclasses to override specific individual steps (hook methods or abstract primitives) to customize specialized behavior.

The algorithm's invariant structure (pre-checks, transaction management, error logging, and metrics recording) remains locked in the base class, preventing subclasses from accidentally omitting critical operational steps.

## Problem being solved

Implementing multi-step workflows across different domain variants without template methods leads to code duplication and missing steps:

- Inconsistent error handling: Multiple report generators implement their own CSV streaming loops; some remember to close file streams and log completion metrics, while others omit error handling.
- Duplicated transaction boilerplate: Every state transition handler duplicates identical transaction management and outbox insertion steps.
- Fragile workflow modifications: Changing the overarching security audit requirements requires editing every single workflow handler across the codebase.

## Simple mental model

The Template Method is like a standard corporate procurement form or cake baking recipe.

The recipe sets the unchangeable order of operations: Step 1: Preheat oven; Step 2: Mix dry ingredients; Step 3: Add flavorings; Step 4: Bake for 30 minutes; Step 5: Cool on rack.

You can customize Step 3 to add chocolate chips or blueberries, but you cannot bake before mixing, nor can you skip preheating the oven.

## How it works

A Template Method architecture consists of:

1. Abstract base class: Declares a `public final` template method defining the immutable algorithm sequence (e.g., `executeWorkflow()`).
1. Invariant steps: Private or final methods in the base class that execute identical operations across all variants (such as tenant validation and audit logging).
1. Abstract primitive steps: Abstract methods that subclasses must implement to provide specialized business logic (e.g., `calculateLineItems()`).
1. Optional hook methods: Empty or default methods that subclasses may selectively override to alter optional behavior (e.g., `onPostExecution()`).
1. Subclasses: Concrete implementations that supply specific step logic without altering the execution sequence.

## Why HRFlow needs it

In HRFlow's reporting and workflow processing:

- Multi-format report generation: The `reporting` package defines an abstract `ReportGenerator` base class that manages tenant verification, date range validation, query paging, and audit logging. Subclasses (`AttendanceSummaryReport`, `LeaveUtilizationReport`) only implement row formatting.
- State transition handlers: Ensuring that all workflow transitions enforce tenant authorization, pessimistic balance checks, outbox event insertion, and audit logging in a guaranteed sequence.
- File import parsers: Reading CSV rosters, parsing employee rows, validating field schemas, and inserting database records.

## Minimal example

Template Method pattern in Java:

```java
// Abstract base class with invariant template method
public abstract class AbstractReportGenerator {

    // Final template method enforces execution sequence
    public final byte[] generateReport(String tenantId, LocalDate startDate, LocalDate endDate) {
        validateInputs(tenantId, startDate, endDate);
        List<Map<String, Object>> data = fetchData(tenantId, startDate, endDate);
        byte[] formattedOutput = formatOutput(data);
        recordAuditLog(tenantId, data.size());
        return formattedOutput;
    }

    private void validateInputs(String tenantId, LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot precede start date");
        }
    }

    // Abstract primitive steps implemented by subclasses
    protected abstract List<Map<String, Object>> fetchData(String tenantId,
                                                            LocalDate start,
                                                            LocalDate end);
    protected abstract byte[] formatOutput(List<Map<String, Object>> data);

    // Invariant step handled by base class
    private void recordAuditLog(String tenantId, int rowCount) {
        // Record audit event in reporting.audit_logs
    }
}
```

## Application-specific implementation

Concrete report implementations in HRFlow:

```java
public class AttendanceCsvReportGenerator extends AbstractReportGenerator {

    private final AttendanceRepository repository;

    public AttendanceCsvReportGenerator(AttendanceRepository repository) {
        this.repository = repository;
    }

    @Override
    protected List<Map<String, Object>> fetchData(String tenantId, LocalDate start, LocalDate end) {
        return repository.findDailySummaries(tenantId, start, end);
    }

    @Override
    protected byte[] formatOutput(List<Map<String, Object>> data) {
        StringBuilder csv = new StringBuilder("Employee,Date,Status,Hours\n");
        for (Map<String, Object> row : data) {
            csv.append(row.get("employee")).append(",")
               .append(row.get("date")).append(",")
               .append(row.get("status")).append(",")
               .append(row.get("hours")).append("\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
```

## Common mistakes and pitfalls

Avoid these Template Method pitfalls:

> **Pitfall:** Forgetting to mark the template method as `final`. If the main workflow method `generateReport()` is not `final`, a subclass can override it completely, inadvertently bypassing critical security validations and audit logging steps. Always declare template methods `public final`.

- Deep inheritance hierarchies: Stacking three or four layers of abstract classes (`BaseReport` -> `AbstractDatabaseReport` -> `AbstractCsvReport` -> `ConcreteReport`) makes code difficult to trace and maintain. Keep template hierarchies strictly one level deep.
- Leaking mutable protected state: Exposing mutable protected fields to subclasses invites unpredictable side effects. Keep base class state private and pass data between steps using method parameters and return values.
- Forcing inheritance over composition: If the algorithm's sequence varies fundamentally, prefer Strategy or Chain of Responsibility rather than rigid inheritance.

## Testing and debugging

Validation routine:

- Invariant enforcement test: Create a test subclass and pass an invalid date range. Verify that the base class throws `IllegalArgumentException` before `fetchData()` is ever invoked.
- Subclass unit test: Test `AttendanceCsvReportGenerator` by invoking `formatOutput()` directly (if package-private) or testing the complete template execution with mocked repository data.
- Audit log execution assertion: Verify that every execution of the template method records an audit entry regardless of which subclass was executed.

## Trade-offs and alternatives

Workflow structuring approaches:

- Template Method pattern (HRFlow choice for standardized workflows): Guarantees invariant execution order, eliminates duplicate lifecycle boilerplate, simple to understand; couples subclasses to base class hierarchy.
- Strategy pattern: Uses composition instead of inheritance; algorithms can be swapped dynamically at runtime, but does not enforce a rigid multi-step execution skeleton.
- Functional callback pipelines: Passing lambdas (`executeWithAudit(() -> doWork())`); concise, but can be harder to browse across multiple complex steps.

## When not to use it

Do not use the Template Method pattern for:

- Algorithms with dynamic step counts: If an algorithm needs a variable number of steps configured at runtime, use Chain of Responsibility instead.
- Simple single-method variations: Use Strategy with lambdas for single-method customizations.

## Related documentation

- [Strategy pattern primer](STRATEGY.md)
- [Chain of responsibility primer](CHAIN-OF-RESPONSIBILITY.md)
- [Patterns learning path](../PATTERNS-LEARNING-PATH.md)
- [Week 09 reporting and audit build guide](../09-REPORTING-AUDIT-OBSERVABILITY/BUILD-GUIDE.md)
