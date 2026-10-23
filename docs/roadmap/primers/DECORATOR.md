# Decorator pattern and cross-cutting behavior

> **Note:** The Decorator pattern attaches additional responsibilities to an object dynamically, providing a flexible alternative to subclassing for extending functionality.

## Purpose and core concept

The Decorator pattern wraps an existing object inside another object that implements the exact same interface.

The decorator intercepts calls to the underlying object's methods, executes additional behavior before or after delegation (such as caching, metrics collection, audit logging, or retry logic), and returns the result without altering the original class's code.

## Problem being solved

Adding cross-cutting concerns (such as caching, timing, or transaction retry) directly into core business services causes severe code tangling:

- Polluted business methods: A leave calculation service method becomes overwhelmed with stopwatch timers, cache lookup checks, cache eviction logic, and try-catch retry loops, obscuring the actual business formula.
- Class explosion through inheritance: Using class inheritance to create variants (`CachedLeaveService`, `LoggedLeaveService`, `CachedAndLoggedLeaveService`) causes an explosion of subclasses.
- Rigid compile-time extension: Inheritance statically binds behavior at compile time, whereas decorators allow dynamic stacking at runtime.

## Simple mental model

The Decorator pattern is like putting on layers of clothing for winter weather.

You are the core person. Putting on a thermal undershirt adds warmth without changing who you are. Putting on a waterproof raincoat over the shirt adds rain protection.

Each clothing layer wraps around the previous layer, adding specialized protection while presenting the same human interface to the outside world.

## How it works

A standard Decorator architecture features:

1. Component interface: The common interface shared by both the original service and all decorators (e.g., `EmployeeDirectory` declaring `getEmployee(id)`).
1. Concrete component: The clean, core business implementation class containing zero cross-cutting clutter.
1. Base decorator class: An abstract or concrete class implementing the component interface that holds a reference to a wrapped component and delegates all calls to it.
1. Concrete decorators: Classes extending the base decorator that add specific behaviors (such as `CachingEmployeeDirectoryDecorator` or `AuditingEmployeeDirectoryDecorator`).

## Why HRFlow needs it

In HRFlow's final service architecture:

- Caching attendance summary projections: Wrapping the database attendance repository with a caching decorator that caches daily summaries in memory without touching repository code.
- Audit logging: Wrapping command handlers with an auditing decorator that writes audit entries to `reporting.audit_logs` upon successful execution.
- Resilience and retries: Wrapping external network adapters (such as SMS or push notification dispatchers) with an automated retry decorator.

## Minimal example

Decorator pattern in Java:

```java
// Common component interface
public interface EmployeeService {
    Employee findById(String tenantId, UUID id);
}

// Core business implementation
@Service
public class DefaultEmployeeService implements EmployeeService {
    private final EmployeeRepository repository;

    public DefaultEmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    public Employee findById(String tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
    }
}

// Caching decorator wrapping EmployeeService
public class CachingEmployeeServiceDecorator implements EmployeeService {
    private final EmployeeService delegate;
    private final Map<UUID, Employee> cache = new ConcurrentHashMap<>();

    public CachingEmployeeServiceDecorator(EmployeeService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Employee findById(String tenantId, UUID id) {
        return cache.computeIfAbsent(id, key -> delegate.findById(tenantId, key));
    }
}
```

## Application-specific implementation

Spring `@Primary` decorator wiring in HRFlow:

```java
@Configuration
public class ServiceDecoratorConfiguration {

    @Bean
    @Primary
    public EmployeeService cachedEmployeeService(DefaultEmployeeService coreService) {
        // Wrap the clean core service with the caching decorator
        return new CachingEmployeeServiceDecorator(coreService);
    }
}
```

Caller classes simply inject `EmployeeService`. Spring injects the `@Primary` decorated bean, giving callers transparent caching without any caller awareness.

## Common mistakes and pitfalls

Avoid these decorator traps:

> **Pitfall:** Breaking the Liskov Substitution Principle. A decorator must adhere strictly to the contract of the interface it wraps. A decorator must never throw unexpected checked exceptions or return altered business datatypes that the underlying interface does not permit.

- Decorator ordering dependencies: When stacking multiple decorators (e.g., Caching + Logging + Security), order matters. Applying Caching outside Security could allow unauthorized users to receive cached responses without triggering security checks. Always apply Security innermost or outermost deliberately.
- Accidental cache staleness: Caching decorators that fail to evict cache entries upon update mutations (`save()` or `delete()`) return stale data to users.
- Overusing decorators instead of Spring AOP: In Spring Boot, simple method execution timing or declarative `@Transactional` boundaries are cleaner with standard Spring Aspect-Oriented Programming (AOP) annotations rather than manual decorator classes.

## Testing and debugging

Validation routine:

- Core service isolation test: Unit test `DefaultEmployeeService` with mocked repositories to verify core retrieval logic with zero caching interference.
- Decorator behavior test: Mock the delegate `EmployeeService`. Call the decorator twice with the same ID. Assert that the underlying delegate's `findById()` method was invoked exactly once (verifying cache hit on the second call).
- Cache eviction test: Mutate the entity and assert that subsequent reads bypass the cache and fetch fresh data.

## Trade-offs and alternatives

Behavior extension approaches:

- Decorator pattern (HRFlow choice for clean programmatic wrapping): Explicit, transparent, easy to test, conforms to standard interface contracts; requires writing interface delegation boilerplate.
- Spring AOP (`@Aspect`): Powerful bytecode interception without interface boilerplate; can be obscure to debug and does not work on internal method calls (`this.method()`).
- Class inheritance: Subclassing the original service; rigid, tightly coupled, and leads to class explosion.

## When not to use it

Do not use the Decorator pattern for:

- Fundamental alterations to business logic: If an operation requires fundamentally different calculation algorithms, use Strategy or Template Method instead.
- Simple single-method utility extensions: Static helper methods are simpler than creating multi-class decorator hierarchies.

## Related documentation

- [Strategy pattern primer](STRATEGY.md)
- [Adapter pattern primer](ADAPTER.md)
- [Patterns learning path](../PATTERNS-LEARNING-PATH.md)
- [Week 09 reporting and audit build guide](../09-REPORTING-AUDIT-OBSERVABILITY/BUILD-GUIDE.md)
