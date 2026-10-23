# Specification pattern and composable business rules

> **Note:** HRFlow ADR 008 chooses explicit JDBC. Any JPA example below is comparative teaching only, never the selected implementation. Use the Spring JDBC primer and owner-scoped SQL; no extra ORM dependency is admitted.

> **Note:** The Specification pattern encapsulates business rules and validation predicates into reusable, composable boolean criteria objects that can be combined using logical operators.

## Purpose and core concept

The Specification pattern represents a business rule as a discrete object implementing a single boolean test method: `boolean isSatisfiedBy(T candidate)`.

Instead of scattering complex validation rules across multiple services or writing monolithic SQL queries, specifications allow developers to name, isolate, and combine rules using logical operations (`and`, `or`, `not`).

## Problem being solved

Managing complex domain eligibility criteria without specifications leads to severe code duplication and bugs:

- Scattered validation predicates: Rules determining whether an employee is eligible for paid parental leave are duplicated across the web controller, mobile API, and reporting batch jobs.
- Complex nested conditional logic: Long chains of nested `if-else` blocks (`if (emp.isActive() && (emp.getTenure() > 12 || emp.isExempt()) && ...)`) are difficult to read, maintain, and unit test.
- Leaked domain logic into database queries: Writing complex dynamic WHERE clauses in repositories duplicates domain knowledge in SQL strings.

## Simple mental model

The Specification pattern is like a set of specialized puzzle piece templates used by a quality inspector on an assembly line.

Each template tests a specific physical property: one template checks if the piece is wider than 10 centimeters; another checks if the piece is painted blue. The inspector can easily stack templates together to test: "Is this piece wider than 10 centimeters AND painted blue?"

## How it works

A classic Specification pattern contains:

1. Specification interface: Declares `boolean isSatisfiedBy(T candidate)` and combinator methods (`and`, `or`, `not`).
1. Concrete specifications: Classes encapsulating single domain rules (e.g., `ActiveEmployeeSpecification`, `SufficientTenureSpecification`).
1. Composite specifications: Classes combining two sub-specifications using boolean logic (`AndSpecification`, `OrSpecification`).
1. Evaluation: The domain service passes the candidate entity to the composed specification to make an unambiguous business decision.

## Why HRFlow needs it

In HRFlow's Week 07 curriculum and business workflows:

- Leave eligibility validation: Complex leave policies (such as verifying minimum employment tenure, active employee status, and absence of probation restrictions) are modeled as composable specifications.
- Material request approvals: Determining whether a request requires department head approval versus executive approval based on item cost and department budget.
- Shift anomaly detection: Evaluating whether an attendance session qualifies as `NEEDS_REVIEW` based on multiple overlapping shift criteria.

## Minimal example

Composable Specification in Java:

```java
public interface Specification<T> {
    boolean isSatisfiedBy(T candidate);

    default Specification<T> and(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
    }

    default Specification<T> or(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
    }

    default Specification<T> not() {
        return candidate -> !this.isSatisfiedBy(candidate);
    }
}
```

Concrete implementations:

```java
public class ActiveEmployeeSpec implements Specification<Employee> {
    @Override
    public boolean isSatisfiedBy(Employee candidate) {
        return "ACTIVE".equalsIgnoreCase(candidate.getStatus());
    }
}

public class ProbationPassedSpec implements Specification<Employee> {
    @Override
    public boolean isSatisfiedBy(Employee candidate) {
        return candidate.getEmploymentMonths() >= 3;
    }
}
```

## Application-specific implementation

Composed policy evaluation in HRFlow's leave domain:

```java
@Service
public class LeavePolicyService {

    public boolean canRequestAnnualLeave(Employee employee) {
        Specification<Employee> eligibilityRule = new ActiveEmployeeSpec()
                .and(new ProbationPassedSpec());

        return eligibilityRule.isSatisfiedBy(employee);
    }

    public boolean requiresExecutiveApproval(MaterialRequest request) {
        Specification<MaterialRequest> highValue = req -> req.getTotalAmount() > 5000.0;
        Specification<MaterialRequest> restrictedCategory =
            req -> "IT_EQUIPMENT".equals(req.getCategory());

        return highValue.or(restrictedCategory).isSatisfiedBy(request);
    }
}
```

Spring Data JPA also provides built-in `org.springframework.data.jpa.domain.Specification` to translate these specifications directly into dynamic database queries using the JPA Criteria API.

## Common mistakes and pitfalls

Avoid these specification pitfalls:

> **Pitfall:** Performing database queries or external network calls inside `isSatisfiedBy()`. A specification should operate purely as an in-memory boolean predicate on an already loaded entity or DTO. Performing database lookups inside a specification causes severe N+1 query performance degradation.

- Over-abstraction for single simple checks: Creating a separate specification class for a trivial check like `status.equals("ACTIVE")` when it is used in only one place adds unnecessary file overhead. Use specifications when rules are reused or composed.
- Mixing evaluation with side effects: A specification must never mutate candidate state or trigger database updates; it must remain a pure, side-effect-free function returning a boolean.
- Complex multi-entity specifications: Passing five disparate entities into a single specification indicates that the domain model is poorly factored. Keep the candidate parameter focused.

## Testing and debugging

Validation routine:

- Isolated rule unit test: Test `ActiveEmployeeSpec` with active and inactive employee fixtures. Verify that it returns `true` and `false` as expected.
- Composite logic assertion: Test the composed `and()` specification with an active employee who has not passed probation. Verify that the combined rule returns `false`.
- Fast execution: Because specifications operate purely in memory, thousands of specification unit tests can execute in milliseconds.

## Trade-offs and alternatives

Architecture comparison:

- Specification pattern (HRFlow choice for complex policies): Composable, highly readable, independently testable, reusable; introduces extra classes for individual rules.
- Plain service methods: Fast to write initially; quickly degenerates into long, untestable conditional statements.
- External rules engine (Drools): Powerful dynamic rule evaluation for hundreds of complex business rules; heavy runtime footprint, steep learning curve, and complex debugging.

## When not to use it

Do not use the Specification pattern for:

- Simple single-parameter validations: Checking whether an email address format is valid or a field is not blank is better handled by standard Bean Validation annotations (`@NotBlank`, `@Email`).
- Database mutations: Operations that modify or delete records.

## Related documentation

- [Pattern lesson week 07](../07-REQUESTS-AND-APPROVALS/PATTERN-LESSON.md)
- [Patterns learning path](../PATTERNS-LEARNING-PATH.md)
- [Builder pattern primer](BUILDER.md)
- [Week 07 requests and approvals build guide](../07-REQUESTS-AND-APPROVALS/BUILD-GUIDE.md)
