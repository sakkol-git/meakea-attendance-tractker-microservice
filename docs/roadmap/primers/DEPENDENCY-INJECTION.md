# Dependency injection and loose coupling

> **Note:** Dependency Injection (DI) is an implementation of Inversion of Control (IoC) where an object receives its dependencies from an external assembler rather than creating them directly.

## Purpose and core concept

Dependency Injection separates the creation and configuration of dependent objects from their business usage.

Instead of a class instantiating its collaborators using the `new` keyword (e.g., `new DatabaseAttendanceRepository()`), the class declares its required dependencies as constructor arguments. An IoC container (such as Spring's `ApplicationContext`) instantiates the components and supplies (injects) them at startup.

## Problem being solved

Instantiating dependencies directly inside classes leads to tightly coupled, rigid codebases:

- Hardcoded implementations: A service that instantiates `new SmtpNotificationSender()` cannot be switched to an in-app outbox dispatcher without modifying its source code.
- Untestable services: If a class instantiates real database connections or network sockets in its constructor or fields, unit testing without a live database is impossible.
- Hidden dependency graphs: When classes instantiate their own sub-dependencies, the overall architecture and dependency graph cannot be easily visualized or refactored.

## Simple mental model

Dependency injection is like using a standard electrical wall socket and plug.

A lamp does not generate its own electricity, nor is it hardwired into the power plant miles away. Instead, the lamp exposes a standard electrical plug (an interface). You can plug the lamp into a home wall outlet, a portable battery generator, or an uninterruptible power supply (UPS) without changing the lamp's lightbulb or wiring.

## How it works

The dependency injection workflow in Spring Boot:

1. Component registration: Classes intended for dependency injection are annotated with stereotype annotations (`@Service`, `@Repository`, `@Component`) or declared in `@Configuration` classes.
1. Container discovery: On application startup, Spring scans the classpath, constructs a dependency graph, and detects circular dependency errors.
1. Constructor injection: Spring identifies the single constructor of a bean and resolves all required parameter types from the container.
1. Instantiation: Collaborators are instantiated in dependency order and injected into the target bean's constructor.
1. Immutability: Injected dependencies are stored in `private final` fields, ensuring thread safety and immutability.

## Why HRFlow needs it

In HRFlow's final service architecture:

- Interface-based domain contracts: The `leave` package depends on the public interface `EmployeeDirectoryPort` rather than the internal `identityorg.EmployeeRepository` implementation.
- Effortless unit testing: Domain services can be instantiated directly in JUnit tests using plain `new LeaveService(mockRepo, mockOutbox)` without spinning up the Spring container.
- Clean profile swapping: Allows seamless swapping of local development adapters with production services (such as swapping an in-memory mock notification sender for a production outbox dispatcher).

## Minimal example

Constructor dependency injection in Java:

```java
// Dependency interface
public interface NotificationSender {
    void send(String tenantId, String recipient, String message);
}

// Dependent service receiving dependency via constructor
@Service
public class AttendanceReminderService {

    private final NotificationSender notificationSender;

    public AttendanceReminderService(NotificationSender notificationSender) {
        this.notificationSender = Objects.requireNonNull(notificationSender);
    }

    public void remindEmployee(String tenantId, String employeeEmail) {
        notificationSender.send(tenantId, employeeEmail,
                                "Please remember to submit your timesheet.");
    }
}
```

## Application-specific implementation

Domain port abstraction across packages in HRFlow:

In `com.hrflow.app.identityorg.ports`:

```java
public interface EmployeeDirectoryPort {
    boolean isEmployeeActive(String tenantId, UUID employeeId);
    Optional<UUID> findManagerId(String tenantId, UUID employeeId);
}
```

In `com.hrflow.app.leave.service`:

```java
@Service
public class LeaveValidationService {

    private final EmployeeDirectoryPort employeePort;
    private final LeaveBalanceRepository balanceRepo;

    public LeaveValidationService(EmployeeDirectoryPort employeePort,
                                  LeaveBalanceRepository balanceRepo) {
        this.employeePort = employeePort;
        this.balanceRepo = balanceRepo;
    }

    public void validateLeaveRequest(String tenantId, UUID employeeId, double requestedDays) {
        if (!employeePort.isEmployeeActive(tenantId, employeeId)) {
            throw new InactiveEmployeeException("Cannot request leave for inactive employee");
        }
        // Balance verification continues...
    }
}
```

## Common mistakes and pitfalls

Avoid these common dependency injection mistakes:

> **Pitfall:** Using field injection with `@Autowired private NotificationSender sender;`. Field injection hides dependencies from constructors, makes unit testing without Spring reflection difficult, and allows classes to be instantiated with uninitialized `null` fields. Always use constructor injection.

- Circular dependencies: Class A depends on Class B, while Class B depends on Class A. This indicates poor domain boundary design. Break cycles by introducing events or intermediate port interfaces.
- Fat constructors: A service requiring 10 constructor parameters violates the Single Responsibility Principle. Break oversized services into smaller, focused domain services.
- Injecting stateful entities: Never inject JPA `@Entity` instances or request-scoped mutable objects as singleton Spring beans.

## Testing and debugging

Verification routines:

- Pure unit testing: Test `LeaveValidationService` without Spring annotations:

```java
@Test
void shouldRejectLeaveWhenEmployeeIsInactive() {
    EmployeeDirectoryPort mockPort = Mockito.mock(EmployeeDirectoryPort.class);
    LeaveBalanceRepository mockRepo = Mockito.mock(LeaveBalanceRepository.class);
    when(mockPort.isEmployeeActive("tenant-a", empId)).thenReturn(false);

    LeaveValidationService service = new LeaveValidationService(mockPort, mockRepo);

    assertThrows(InactiveEmployeeException.class,
                 () -> service.validateLeaveRequest("tenant-a", empId, 2.0));
}
```

- Context loading verification: Run `mvn test -Dtest=ApplicationContextTest` to verify that all Spring bean dependencies resolve cleanly without circular dependency errors.

## Trade-offs and alternatives

Architecture comparison:

- Constructor DI (HRFlow choice): Explicit dependencies, immutable fields, effortless unit testing; requires writing constructor parameters (or using Lombok `@RequiredArgsConstructor`).
- Service Locator pattern: Classes query a central registry (`ServiceLocator.get(Service.class)`); hides dependencies and couples code to a static locator registry.
- Manual wiring: Constructing the entire dependency graph manually in `main()`; cumbersome and verbose in large applications.

## When not to use it

Do not use dependency injection for:

- Transient value objects: Entities, DTOs, records, and domain primitives (such as `AttendanceSession`, `Money`, `DateRange`) should be instantiated directly using constructors or builders.
- Pure utility functions: Static stateless helper methods (such as `DateUtils.calculateWorkDays()`) require no instance state or injection.

## Related documentation

- [Repository pattern primer](REPOSITORY.md)
- [Adapter pattern primer](ADAPTER.md)
- [Builder pattern primer](BUILDER.md)
- [Week 01 foundation build guide](../01-FOUNDATION/BUILD-GUIDE.md)
