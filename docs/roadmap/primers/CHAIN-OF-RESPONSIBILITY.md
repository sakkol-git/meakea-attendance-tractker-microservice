# Chain of Responsibility and validation pipelines

> **Note:** The Chain of Responsibility pattern passes a request along a dynamic chain of handlers. Upon receiving a request, each handler decides either to process the request or to pass it to the next handler in the chain.

## Purpose and core concept

The Chain of Responsibility decouples the sender of a request from its potential receivers. Multiple handler objects are linked into a linear pipeline.

Each handler evaluates the request, optionally performs validation or transformation, and either short-circuits the pipeline (by throwing an error or returning early) or delegates execution forward to the next successor in the chain.

## Problem being solved

Validating complex domain operations (such as submitting an attendance correction or approving an expensive equipment purchase) involves multiple sequential checks:

- Monolithic validation methods: Stacking twenty distinct validation checks into a single 200-line service method produces tightly coupled code where reordering checks or adding new rules requires risky edits to existing code.
- Hardcoded pipeline flow: Clients cannot easily reconfigure which validation steps execute for different tenants or request types.
- Poor error isolation: Testing individual validation rules in isolation is difficult when all checks are tangled inside one procedural method.

## Simple mental model

Chain of Responsibility is like an airport security screening checkpoint.

Passengers step through sequential stations in a fixed line: first, a document clerk checks your passport and boarding pass; second, you place your luggage through the X-ray scanner; third, you walk through the metal detector.

If your passport is expired, the document clerk stops you immediately; you never reach the metal detector. If your documents are valid, you proceed smoothly to the next station.

## How it works

A standard Chain of Responsibility architecture features:

1. Handler interface or abstract class: Declares a method for handling requests (e.g., `void validate(LeaveRequest request)`) and a method to set the next successor in the chain (`setNext(Handler next)`).
1. Concrete handlers: Classes encapsulating single validation steps (e.g., `DateRangeValidator`, `BalanceAvailabilityValidator`, `BlackoutPeriodValidator`).
1. Pipeline assembly: Handlers are linked together sequentially either manually or via a pipeline builder.
1. Sequential execution: The client invokes the first handler. Each handler executes its check and calls `next.validate(request)` if the check passes.
1. Short-circuiting: If any handler detects an invalid condition, it raises a domain exception, halting downstream processing immediately.

## Why HRFlow needs it

In HRFlow's multi-step validation workflows:

- Request approval validation: Before submitting a material request, the request passes through sequential checks: budget limit check -> vendor authorization check -> department inventory stock check.
- Attendance check-in validation: Check-ins pass through a chain: tenant active check -> employee active check -> shift schedule match -> geofence verification.
- HTTP security filter chains: Spring Security's internal filter chain is a classic Chain of Responsibility (JWT filter -> tenant filter -> CORS filter -> authorization filter).

## Minimal example

Chain of Responsibility in Java:

```java
// Abstract handler
public abstract class RequestValidator {
    private RequestValidator next;

    public RequestValidator linkWith(RequestValidator next) {
        this.next = next;
        return next;
    }

    public abstract void validate(LeaveRequest request);

    protected void checkNext(LeaveRequest request) {
        if (next != null) {
            next.validate(request);
        }
    }
}

// Concrete handler 1: Date sanity check
public class DateSanityValidator extends RequestValidator {
    @Override
    public void validate(LeaveRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("End date cannot precede start date");
        }
        checkNext(request);
    }
}

// Concrete handler 2: Blackout period check
public class BlackoutPeriodValidator extends RequestValidator {
    @Override
    public void validate(LeaveRequest request) {
        if (request.fallsOnCompanyBlackoutPeriod()) {
            throw new ValidationException("Leave cannot be booked during blackout period");
        }
        checkNext(request);
    }
}
```

## Application-specific implementation

Pipeline assembly and execution in HRFlow:

```java
@Service
public class LeaveSubmissionPipeline {

    private final RequestValidator validationChain;

    public LeaveSubmissionPipeline() {
        RequestValidator first = new DateSanityValidator();
        first.linkWith(new BlackoutPeriodValidator())
             .linkWith(new MinimumAdvanceNoticeValidator());

        this.validationChain = first;
    }

    public void executePreSubmissionChecks(LeaveRequest request) {
        validationChain.validate(request);
    }
}
```

## Common mistakes and pitfalls

Avoid these chain implementation flaws:

> **Pitfall:** Broken chain links. If a handler author forgets to call `checkNext(request)` at the end of their `validate()` method, the pipeline silently stops, failing to execute all downstream validation steps. Always verify that successor methods are called in all non-exceptional branches.

- Creating infinite recursive loops: Accidentally linking Handler A to Handler B and Handler B back to Handler A causes a `StackOverflowError` upon execution. Chains must be strictly acyclic.
- Handlers that mutate request state unpredictably: If Handler 1 mutates the request entity and Handler 3 relies on the original unmodified values, order-dependent bugs occur. Keep request objects immutable during validation.
- Over-engineering simple two-step checks: If a service only ever needs two simple checks that never change, a simple `if` statement is clearer than a multi-class chain.

## Testing and debugging

Validation routine:

- Isolated handler unit test: Instantiate `DateSanityValidator` in isolation with a `null` successor. Pass a request with an invalid date range and assert that `ValidationException` is thrown.
- Full pipeline integration test: Construct the full chain and pass a valid request. Verify that execution completes with zero exceptions.
- Early short-circuit test: Pass a request that fails the first handler and verify that the second handler is never invoked (verified via Mockito verify).

## Trade-offs and alternatives

Validation pipeline approaches:

- Chain of Responsibility (HRFlow choice for sequential validation): Decouples individual validation rules, respects Single Responsibility Principle, easily reconfigurable; introduces multiple classes and requires care to avoid broken links.
- Monolithic validator class: A single class with twenty methods; simple to browse in one file, but difficult to test and maintain as rules grow.
- Specification pattern: Focuses on combining boolean criteria using logical operators (`and`, `or`); unlike Chain, specifications do not manage sequential pipeline execution or short-circuit exceptions.

## When not to use it

Do not use Chain of Responsibility for:

- Independent validations that should accumulate all errors: If you want to return a list of all twenty form errors to the user at once (rather than stopping at the first error), use a form validation collector instead.
- Simple single-line validations: Checking a non-null parameter.

## Related documentation

- [Specification pattern primer](SPECIFICATION.md)
- [Decorator pattern primer](DECORATOR.md)
- [Patterns learning path](../PATTERNS-LEARNING-PATH.md)
- [Week 07 requests and approvals build guide](../07-REQUESTS-AND-APPROVALS/BUILD-GUIDE.md)
