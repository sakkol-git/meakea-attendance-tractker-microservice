# Strategy pattern and interchangeable algorithms

> **Note:** The Strategy pattern defines a family of algorithms, encapsulates each algorithm in a separate class, and makes them interchangeable at runtime.

## Purpose and core concept

The Strategy pattern enables an algorithm's implementation to vary independently from the clients that use it.

Instead of writing a giant method full of complex `if-else` or `switch` statements that choose between different ways of performing an operation (such as calculating shift overtime or validating passwords), each algorithm is encapsulated inside a dedicated strategy class implementing a common interface.

## Problem being solved

Embedding multiple algorithmic variants directly inside service classes creates brittle, monolithic code:

- Bloated calculation methods: An overtime calculator that contains custom rules for standard day shifts, night differential shifts, holiday shifts, and weekend shifts in one giant 300-line method is nearly impossible to understand or maintain.
- Regression risk: Modifying weekend overtime calculation logic requires editing the same source file where regular weekday overtime is computed, risking accidental regressions in unrelated shift types.
- Violation of the Open-Closed Principle: Adding a new shift rule requires modifying existing, tested classes rather than adding a new strategy class.

## Simple mental model

The Strategy pattern is like choosing a travel navigation route on a smartphone mapping app.

When you enter a destination, the app lets you choose your travel strategy: "Driving", "Walking", "Bicycling", or "Public Transit". The destination remains identical, but the internal routing algorithm calculates travel duration, turns, and tolls completely differently depending on the selected strategy.

## How it works

A Strategy implementation consists of three roles:

1. Strategy interface: Declares the common method signature for the algorithm (e.g., `OvertimeCalculationStrategy` declaring `double computeOvertime(AttendanceSession session)`).
1. Concrete strategies: Individual classes encapsulating specific algorithmic variations (e.g., `StandardShiftStrategy`, `NightDifferentialStrategy`, `PublicHolidayStrategy`).
1. Context class: The business service that maintains a reference to a strategy object and delegates algorithmic execution to it.

## Why HRFlow needs it

In HRFlow's attendance and overtime domain:

- Shift compensation rules: Organizations configure diverse compensation rules based on local labor laws and employment contracts. Strategy classes calculate regular overtime, night shifts, and rest-day premiums independently.
- Password hashing policies: Migrating from legacy BCrypt to Argon2id password hashing is implemented using interchangeable hashing strategies.
- Notification dispatching: Delivering alerts through email, SMS, or in-app inbox channels.

## Minimal example

Strategy pattern in Java:

```java
// Strategy interface
public interface OvertimeStrategy {
    double calculateOvertimeHours(double workedHours, double scheduledHours);
}

// Concrete strategy 1: Standard daily overtime
public class StandardOvertimeStrategy implements OvertimeStrategy {
    @Override
    public double calculateOvertimeHours(double workedHours, double scheduledHours) {
        return Math.max(0.0, workedHours - scheduledHours);
    }
}

// Concrete strategy 2: Double-time holiday strategy
public class HolidayOvertimeStrategy implements OvertimeStrategy {
    @Override
    public double calculateOvertimeHours(double workedHours, double scheduledHours) {
        return workedHours * 2.0; // All hours on holiday count double
    }
}
```

## Application-specific implementation

Context service selecting strategy at runtime in HRFlow:

```java
@Service
public class AttendanceSettlementService {

    private final Map<ShiftType, OvertimeStrategy> strategies;

    public AttendanceSettlementService() {
        this.strategies = Map.of(
            ShiftType.STANDARD, new StandardOvertimeStrategy(),
            ShiftType.HOLIDAY, new HolidayOvertimeStrategy()
        );
    }

    public double calculateSessionOvertime(AttendanceSession session) {
        OvertimeStrategy strategy = strategies.getOrDefault(
            session.getShiftType(),
            new StandardOvertimeStrategy()
        );

        return strategy.calculateOvertimeHours(
            session.getActualWorkedHours(),
            session.getScheduledHours()
        );
    }
}
```

## Common mistakes and pitfalls

Avoid these strategy implementation mistakes:

> **Pitfall:** Passing the entire database context or Spring service container into the strategy. A strategy should be a pure, lightweight algorithm operating strictly on the input data passed to its method. Keep strategies stateless and side-effect-free.

- Creating strategies for non-varying algorithms: If an operation has only one invariant way of executing, encapsulating it in a strategy class is over-engineering. Introduce strategies only when real variations exist.
- Leaking strategy selection to clients: Forcing client controllers or frontend views to know and select internal strategy class names couples clients to backend implementation details. Have the backend service resolve the strategy from domain state.
- Stateful strategies: If a strategy holds mutable instance fields, concurrent requests will experience race conditions. Keep strategies thread-safe and stateless.

## Testing and debugging

Validation routine:

- Isolated algorithm unit test: Instantiate `HolidayOvertimeStrategy` directly and pass test fixtures. Verify that worked hours are doubled exactly without needing any database or Spring context.
- Strategy resolution test: Test that `AttendanceSettlementService` resolves the correct strategy for each enum value in `ShiftType`.
- Boundary testing: Test boundary conditions (such as zero worked hours, negative input values, or partial minutes) against each strategy independently.

## Trade-offs and alternatives

Algorithm design approaches:

- Strategy pattern (HRFlow choice for shift and rate variations): Eliminates conditional branching, isolates algorithmic changes, highly testable in isolation; increases the number of small classes.
- Template Method pattern: Uses inheritance with an abstract base class containing the skeleton algorithm and subclass hooks; less flexible than object composition.
- Lambda functions: For simple calculations, passing a `Function<Double, Double>` lambda is more concise than creating separate named classes.

## When not to use it

Do not use the Strategy pattern for:

- Simple two-branch boolean switches: A simple `if (isFullTime) ... else ...` that changes one number does not warrant two separate strategy classes.
- Operations that change their sequence of execution steps: If the sequence of steps changes fundamentally, use Template Method or Chain of Responsibility instead.

## Related documentation

- [Template method pattern primer](TEMPLATE-METHOD.md)
- [Factory pattern primer](FACTORY.md)
- [Patterns learning path](../PATTERNS-LEARNING-PATH.md)
- [Week 04 attendance core build guide](../04-ATTENDANCE-CORE/BUILD-GUIDE.md)
