# Adapter pattern and external boundary isolation

> **Note:** The Adapter pattern converts the interface of a class into another interface clients expect, allowing classes with incompatible interfaces to collaborate cleanly.

## Purpose and core concept

The Adapter pattern translates between two incompatible software interfaces. It acts as an anti-corruption layer between the clean core domain logic of an application and external third-party libraries, legacy systems, or vendor APIs.

By wrapping an external API inside an adapter that implements a domain-specific interface, the domain remains decoupled from vendor SDKs, proprietary datatypes, and external breaking changes.

## Problem being solved

Directly integrating external third-party SDKs into core business services causes serious architectural entanglement:

- Vendor lock-in: If email notifications directly invoke Twilio SendGrid SDK methods inside domain services, migrating to AWS SES or local SMTP requires modifying core domain classes.
- Contaminated domain models: External SDK datatypes and error exceptions bleed into application services, violating clean architecture boundaries.
- Difficult testability: Directly invoking third-party SDKs prevents straightforward unit testing without mocking complex external vendor objects.

## Simple mental model

The Adapter pattern is like an international electrical travel plug adapter.

When you travel from North America (two flat prongs) to the United Kingdom (three rectangular prongs), you do not rewire your laptop charger or smash the wall socket. Instead, you plug your charger into a small travel adapter that bridges the physical interface mismatch.

## How it works

An Adapter architecture consists of three elements:

1. Target domain interface: The interface required by the client domain (e.g., `NotificationPort` declaring `sendAlert(tenantId, recipient, body)`).
1. Adaptee: The third-party or legacy class with an incompatible interface (e.g., `SendGridClient` requiring `SendGridRequest` objects and API keys).
1. Adapter: A class implementing the target domain interface that holds an instance of the Adaptee, translating domain method parameters into Adaptee calls and converting vendor responses back into domain types.

## Why HRFlow needs it

In HRFlow's modular architecture:

- Notification delivery: The `notification` package defines an internal `NotificationDeliveryPort`. Production code uses an `InAppNotificationAdapter`, while optional extensions can plug in a `PushNotificationAdapter` (FCM/APNs) without changing the core event poller.
- File storage abstraction: The `request` package uses a `FileStoragePort`. Local development uses a `LocalDiskFileAdapter`, while future enterprise tiers could introduce an `S3FileAdapter` without touching request controllers.
- Legacy data import: Importing employee rosters from external CSV spreadsheets or legacy databases is handled by dedicated import adapters.

## Minimal example

Adapter pattern in Java:

```java
// Target interface owned by domain
public interface SmsSender {
    void sendSms(String phoneNumber, String message);
}

// Incompatible external vendor SDK class
public class ThirdPartySmsClient {
    public int dispatchMessage(String apiKey, String intlNumber, byte[] payload) {
        // Vendor-specific implementation
        return 200;
    }
}

// Adapter implementing domain interface
@Component
public class ThirdPartySmsAdapter implements SmsSender {
    private final ThirdPartySmsClient client;
    private final String apiKey;

    public ThirdPartySmsAdapter(ThirdPartySmsClient client,
                                @Value("${sms.api-key}") String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        int statusCode = client.dispatchMessage(apiKey, phoneNumber, payload);
        if (statusCode != 200) {
            throw new NotificationDeliveryException("SMS dispatch failed: " + statusCode);
        }
    }
}
```

## Application-specific implementation

Notification delivery adapter in HRFlow:

In `com.hrflow.app.notification.ports`:

```java
public interface NotificationChannelAdapter {
    boolean supports(NotificationChannel channel);
    void deliver(NotificationMessage message);
}
```

In `com.hrflow.app.notification.adapters`:

```java
@Component
public class InAppNotificationAdapter implements NotificationChannelAdapter {

    private final InAppNotificationRepository inAppRepo;

    public InAppNotificationAdapter(InAppNotificationRepository inAppRepo) {
        this.inAppRepo = inAppRepo;
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.IN_APP;
    }

    @Override
    public void deliver(NotificationMessage message) {
        InAppNotification entity = InAppNotification.builder()
                .id(UUID.randomUUID())
                .tenantId(message.tenantId())
                .userId(message.userId())
                .title(message.title())
                .body(message.body())
                .createdAt(Instant.now())
                .build();
        inAppRepo.save(entity);
    }
}
```

## Common mistakes and pitfalls

Avoid these adapter missteps:

> **Pitfall:** Leaking vendor-specific exceptions out of the adapter into caller services. If an S3 adapter throws an `AmazonS3Exception` directly to the `LeaveService`, the domain becomes coupled to AWS. Always catch vendor exceptions inside the adapter and rethrow clean domain exceptions (such as `StorageUnavailableException`).

- Overcomplicating simple internal calls: Creating an adapter between two internal classes that already have compatible interfaces adds useless boilerplate. Use adapters only when an interface mismatch actually exists.
- Adding business logic inside adapters: Adapters must only translate formats and delegate calls; they must never make business decisions or validate domain rules.
- Stateful adapters: Adapters should be stateless Spring beans that can safely be called by concurrent worker threads.

## Testing and debugging

Validation routine:

- Adapter translation unit test: Mock the external SDK client. Invoke the adapter's domain method and verify that the adapter translated domain parameters into the exact expected vendor method calls.
- Exception mapping test: Force the external SDK mock to throw a connection timeout or vendor error. Verify that the adapter catches the vendor exception and throws the expected domain exception.
- Verification without external network: Unit tests for adapters must run completely offline without attempting to connect to external internet endpoints.

## Trade-offs and alternatives

Architecture comparison:

- Adapter pattern (HRFlow choice for external I/O): Complete isolation from third-party churn, clean test mocking, swappable vendors; requires authoring interface and wrapper classes.
- Direct SDK invocation: Fast initial prototyping; leads to vendor lock-in, hard-to-test services, and domain contamination.
- Facade pattern: Simplifies a complex subsystem behind a single unified interface; unlike an Adapter, it does not adapt to an existing pre-defined interface contract.

## When not to use it

Do not use the Adapter pattern for:

- Internal domain service calls: When Service A calls Service B within the same bounded context, design compatible method signatures directly.
- Standard framework interfaces: Using standard Java or Spring interfaces (like `Map` or `List`) requires no adaptation.

## Related documentation

- [Dependency injection primer](DEPENDENCY-INJECTION.md)
- [Specification pattern primer](SPECIFICATION.md)
- [Patterns learning path](../PATTERNS-LEARNING-PATH.md)
- [Week 05 outbox and notifications build guide](../05-OUTBOX-AND-NOTIFICATIONS/BUILD-GUIDE.md)
