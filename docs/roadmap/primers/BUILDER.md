# Builder pattern and test fixture construction

> **Note:** The Builder pattern separates the construction of a complex object from its representation, allowing step-by-step creation of objects with optional parameters and default values.

## Purpose and core concept

The Builder pattern simplifies the creation of complex objects that possess numerous optional attributes, nested structures, or strict validation requirements.

Instead of writing telescoping constructors with ten overloaded parameters or exposing mutable setter methods, a Builder provides a fluent API where parameters are specified by name and constructed atomically via a terminal `.build()` invocation.

## Problem being solved

Constructing complex domain objects without builders creates brittle, error-prone code:

- Telescoping constructors: Multiple constructors with overlapping parameter lists (`new Employee(id, name)`, `new Employee(id, name, email)`, `new Employee(id, name, email, dept, role)`) lead to confusing method signatures.
- Positional parameter confusion: Passing multiple identical datatypes (such as four consecutive `String` parameters) invites accidental parameter inversion (`new Employee("John", "Doe", "emp@work.com", "Active")` vs `new Employee("Doe", "John", ...)`).
- Test fixture bloat: In unit tests, setting up realistic domain entities requires filling out dozens of mandatory fields even when the test only cares about one specific attribute.

## Simple mental model

The Builder pattern is like ordering a custom sandwich at a delicatessen counter.

You start with a standard roll. You tell the counter clerk exactly which meats, cheeses, vegetables, and condiments you want, in whatever order you recall them. When you finish specifying your choices, the clerk wraps the completed sandwich and hands it to you as an immutable, ready-to-eat meal.

## How it works

A standard Builder implementation contains:

1. Target immutable class: An entity or DTO with private fields and getter methods, but zero public setters.
1. Static nested builder class: A builder class mirroring the target fields with default values for optional properties.
1. Fluent setter methods: Methods returning `this` (the builder instance) to enable method chaining (e.g., `builder.status("ACTIVE").department(deptId)`).
1. Build method: A terminal `build()` method that executes validation invariants and passes the accumulated parameters into the private constructor of the target class.

## Why HRFlow needs it

In HRFlow's test suites and domain models:

- Test fixture builders: Week 03 introduces the Builder pattern specifically to construct employee and shift test fixtures. Tests can specify `EmployeeFixture.builder(tenantId).withDepartment(salesDept).build()`, defaulting irrelevant fields automatically.
- Complex request objects: General material requests encompass multi-line items, attached document metadata, and approval routing lists that benefit from fluent assembly.
- Mandatory tenant enforcement: The builder constructor can mandate the `tenant_id` as a required non-null parameter, preventing the creation of tenant-less fixtures.

## Minimal example

Fluent Builder in Java:

```java
public class EmployeeFixture {
    private final String tenantId;
    private final String fullName;
    private final String email;
    private final String status;

    private EmployeeFixture(Builder builder) {
        this.tenantId = Objects.requireNonNull(builder.tenantId, "Tenant ID required");
        this.fullName = builder.fullName;
        this.email = builder.email;
        this.status = builder.status;
    }

    public static class Builder {
        private final String tenantId;
        private String fullName = "Jane Doe";
        private String email = "jane.doe@example.com";
        private String status = "ACTIVE";

        public Builder(String tenantId) {
            this.tenantId = tenantId;
        }

        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder status(String status) { this.status = status; return this; }

        public EmployeeFixture build() {
            return new EmployeeFixture(this);
        }
    }
}
```

## Application-specific implementation

Unit test fixture setup in HRFlow:

```java
@Test
void shouldCalculateOvertimeForNightShiftEmployee() {
    // Arrange: Only customize shift hours; all other employee attributes use sensible defaults
    Employee employee = EmployeeTestBuilder.aValidEmployee("tenant-corp")
            .withTitle("Operations Lead")
            .withStatus("ACTIVE")
            .build();

    ShiftSession session = ShiftSessionTestBuilder.aSession("tenant-corp")
            .forEmployee(employee.getId())
            .withHours(22, 6) // 10 PM to 6 AM night shift
            .build();

    // Act
    OvertimeCalculation result = calculator.calculate(session);

    // Assert
    assertThat(result.nightDifferentialHours()).isEqualTo(7.0);
}
```

## Common mistakes and pitfalls

Avoid these Builder anti-patterns:

> **Pitfall:** Forcing builders on simple entities that have only two or three required fields. A class with just `id` and `name` is simpler and more concise with a standard Java `record` or constructor. Refactor to a Builder only when optional parameter combinations cause observable friction.

- Mutating builder state after `build()`: Reusing the same builder instance across multiple tests without resetting values causes test cross-contamination. Always instantiate a new builder per test fixture.
- Duplicate validation logic: Validating fields in the builder and repeating identical validation in the entity constructor creates maintenance divergence. Place core business validation in the entity constructor.
- Over-relying on Lombok `@Builder`: Generating builders indiscriminately on all domain entities can bypass domain constructor invariants and encourage anemic domain models.

## Testing and debugging

Validation routine:

- Mandatory parameter test: Attempt to build an entity without passing the required `tenantId`. Verify that the builder throws `NullPointerException` or `IllegalArgumentException`.
- Default values test: Call `new Builder("tenant-a").build()` and assert that all optional fields are populated with valid defaults.
- Fluent chain assertion: Verify that chaining methods (`builder.withA().withB().build()`) sets all fields accurately without dropping values.

## Trade-offs and alternatives

Object creation strategies:

- Builder pattern (HRFlow choice for complex fixtures): Named parameters, fluent readability, immutable results; requires writing extra builder boilerplate classes.
- Telescoping constructors: Zero extra classes; unreadable parameter lists and prone to positional errors.
- Java Records with `with` methods: Clean immutable records; requires re-copying existing fields manually when updating single properties.

## When not to use it

Do not use the Builder pattern for:

- Simple immutable value objects: Java records (e.g., `record Money(BigDecimal amount, String currency)`) are cleaner and more expressive with standard canonical constructors.
- Pure database transfer entities with two fields: Internal join table entities or simple enum mappings.

## Related documentation

- [Pattern lesson week 03](../03-ORGANIZATION-AND-ADMIN/PATTERN-LESSON.md)
- [Patterns learning path](../PATTERNS-LEARNING-PATH.md)
- [Dependency injection primer](DEPENDENCY-INJECTION.md)
- [Week 03 organization build guide](../03-ORGANIZATION-AND-ADMIN/BUILD-GUIDE.md)
