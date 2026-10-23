# Factory pattern and object instantiation

> **Note:** The Factory pattern provides an interface or method for creating objects without specifying the exact concrete class of the object that will be created.

## Purpose and core concept

The Factory pattern delegates the instantiation of complex or polymorphically typed objects to a dedicated creation method or class.

Instead of client code calling `new ConcreteClassA()` or `new ConcreteClassB()` directly based on conditional logic, clients call a factory method (e.g., `LeavePolicyFactory.createPolicy(type)`). The factory encapsulates instantiation details, class selection, and configuration.

## Problem being solved

Instantiating polymorphic objects directly inside service classes creates tight coupling and code duplication:

- Duplicated instantiation switches: Switching on an enum (e.g., `switch (leaveType)`) across five different controllers and services to instantiate policy calculators creates maintenance divergence when a new leave type is added.
- Leaked concrete dependencies: Service classes become coupled to concrete implementation classes rather than high-level interfaces.
- Rigid object creation: Services cannot easily swap or configure creation strategies based on dynamic tenant configuration.

## Simple mental model

A factory is like an automated vehicle assembly plant.

A customer orders a "Sedan", "SUV", or "Pickup Truck" from a catalog. The customer does not need to source individual steel chassis, assemble the gearbox, or wire the engine components. The assembly plant handles the specialized fabrication and delivers a finished, road-ready vehicle conforming to the standard automobile driving controls.

## How it works

A Factory implementation typically features:

1. Common product interface: An interface declaring common operations (e.g., `LeaveAccrualPolicy` declaring `calculateAccrual()`).
1. Concrete products: Classes implementing the interface for specific variants (e.g., `AnnualLeaveAccrualPolicy`, `SickLeaveAccrualPolicy`).
1. Factory method: A method receiving an identifier or configuration and returning an instance of the common interface.
1. Spring bean factory map: In Spring Boot, factories frequently inject a `Map<String, ProductService>` or `List<ProductService>`, allowing automatic registration of new beans without modifying factory code (Open-Closed Principle).

## Why HRFlow needs it

In HRFlow's multi-tenant workforce workflows:

- Diverse leave accrual rules: Different leave types (Annual Leave, Sick Leave, Maternity Leave, Bereavement) calculate monthly accrual, carry-over rules, and maximum balances differently. A factory supplies the appropriate calculation policy based on the leave policy type.
- Notification dispatchers: Instantiating the appropriate delivery strategy (in-app, email, push) based on user channel preferences.
- Report export formatters: Creating CSV, PDF, or Excel export stream writers based on user-requested format parameters.

## Minimal example

Factory pattern in Java:

```java
// Common interface
public interface LeaveAccrualPolicy {
    double calculateMonthlyAccrual(Employee employee);
}

// Concrete variants
public class StandardAnnualAccrual implements LeaveAccrualPolicy {
    @Override
    public double calculateMonthlyAccrual(Employee employee) {
        return 1.25; // 15 days per year
    }
}

public class SeniorStaffAnnualAccrual implements LeaveAccrualPolicy {
    @Override
    public double calculateMonthlyAccrual(Employee employee) {
        return 1.75; // 21 days per year
    }
}

// Factory
public class LeavePolicyFactory {
    public static LeaveAccrualPolicy createPolicy(boolean isSenior) {
        return isSenior ? new SeniorStaffAnnualAccrual() : new StandardAnnualAccrual();
    }
}
```

## Application-specific implementation

Spring-managed strategy factory in HRFlow:

```java
@Component
public class ReportExporterFactory {

    private final Map<String, ReportExporter> exporters;

    // Spring automatically injects all beans implementing ReportExporter into the map
    public ReportExporterFactory(List<ReportExporter> exporterList) {
        this.exporters = exporterList.stream()
                .collect(Collectors.toMap(e -> e.supportedFormat().toUpperCase(), e -> e));
    }

    public ReportExporter getExporter(String format) {
        ReportExporter exporter = exporters.get(format.toUpperCase());
        if (exporter == null) {
            throw new UnsupportedFormatException("Unsupported export format: " + format);
        }
        return exporter;
    }
}
```

## Common mistakes and pitfalls

Avoid these factory design traps:

> **Pitfall:** Creating a Factory class for an object that has only one implementation. If an interface will never have polymorphic variants, adding an abstract factory adds useless indirection and clutter. Use factories only when polymorphic creation actually exists.

- Static factories that cannot be mocked: Using static factory methods with hidden dependencies prevents easy mocking in unit tests. Prefer Spring-managed factory components.
- Putting business logic inside the factory: The factory should only instantiate and configure objects; it must not execute business domain workflows.
- Forgetting fallback handling: Always validate input keys and provide meaningful error exceptions (such as `UnsupportedFormatException`) when an unknown type is requested.

## Testing and debugging

Validation routine:

- Type selection test: Call `factory.getExporter("CSV")` and assert that the returned instance is of type `CsvReportExporter`.
- Unknown type error test: Call `factory.getExporter("UNKNOWN")` and assert that the factory throws `UnsupportedFormatException`.
- Unit test mocking: Mock the factory in service tests to return a lightweight test double product without instantiating complex concrete classes.

## Trade-offs and alternatives

Object instantiation approaches:

- Factory pattern (HRFlow choice for polymorphic strategies): Decouples callers from concrete types, respects Open-Closed Principle, clean extension point; adds an extra factory class.
- Direct `new` invocation: Simplest approach; violates open-closed principle when new variants are added.
- Builder pattern: Focuses on step-by-step construction of complex objects with many optional fields; unlike Factory, it does not focus on polymorphic type selection.

## When not to use it

Do not use the Factory pattern for:

- Simple single-class instantiation: Instantiating a plain DTO or single entity with known fields is cleaner with a standard constructor.
- Configuration beans: Spring `@Configuration` classes with `@Bean` methods already serve as inversion-of-control factories.

## Related documentation

- [Builder pattern primer](BUILDER.md)
- [Strategy pattern primer](STRATEGY.md)
- [Patterns learning path](../PATTERNS-LEARNING-PATH.md)
- [Week 03 organization build guide](../03-ORGANIZATION-AND-ADMIN/BUILD-GUIDE.md)
