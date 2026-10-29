package com.hrflow.identityorg.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hrflow.identityorg.auth.entity.AuthAttempt;
import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.auth.repository.AuthAttemptRepository;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import com.hrflow.identityorg.auth.service.impl.AttemptThrottlerServiceImpl;
import com.hrflow.identityorg.auth.service.record.AttemptTicket;
import org.junit.jupiter.api.Test;

class AttemptThrottlerServiceImplTest {
    @Test
    void fifthFailureBlocksAndTheWindowLaterClearsTheCounter() {
        AuthAttemptRepository repository = mock(AuthAttemptRepository.class);
        Instant now = Instant.parse("2026-10-25T12:00:00Z");
        java.util.Map<String, AuthAttempt> rows = new java.util.HashMap<>();
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            rows.putIfAbsent(id, new AuthAttempt(id, now));
            return null;
        }).when(repository).ensure(any(), any());
        when(repository.findLocked(any())).thenAnswer(invocation -> Optional.of(rows.get(invocation.getArgument(0))));
        AttemptThrottlerService limiter = new AttemptThrottlerServiceImpl(repository, Clock.fixed(now, ZoneOffset.UTC), 5, 50);
        AttemptTicket ticket = null;
        for (int index = 0; index < 4; index++) {
            ticket = limiter.acquire("192.0.2.1", "demo", "user@example.test");
            limiter.failed(ticket);
        }
        AttemptTicket fifth = limiter.acquire("192.0.2.1", "demo", "user@example.test");
        AuthFailedException blocked = assertThrows(AuthFailedException.class, () -> limiter.failed(fifth));
        assertEquals(429, blocked.status.value());
        assertEquals(900, blocked.retryAfterSeconds);
        assertEquals(5, fifth.account().getFailures());
        assertTrue(rows.keySet().stream().allMatch(id -> id.matches("[0-9a-f]{64}")));

        AttemptThrottlerService afterWindow = new AttemptThrottlerServiceImpl(repository,
                Clock.fixed(now.plusSeconds(901), ZoneOffset.UTC), 5, 50);
        AttemptTicket permitted = afterWindow.acquire("192.0.2.1", "demo", "user@example.test");
        assertEquals(0, permitted.account().getFailures());
        verify(repository, times(12)).ensure(any(), any());
    }

    @Test
    void successfulAccountLoginDoesNotClearTheSharedSourceFailures() {
        AuthAttemptRepository repository = mock(AuthAttemptRepository.class);
        Instant now = Instant.parse("2026-10-25T12:00:00Z");
        java.util.Map<String, AuthAttempt> rows = new java.util.HashMap<>();
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            rows.putIfAbsent(id, new AuthAttempt(id, now));
            return null;
        }).when(repository).ensure(any(), any());
        when(repository.findLocked(any())).thenAnswer(invocation -> Optional.of(rows.get(invocation.getArgument(0))));
        AttemptThrottlerService limiter = new AttemptThrottlerServiceImpl(
                repository, Clock.fixed(now, ZoneOffset.UTC), 5, 50);

        AttemptTicket failed = limiter.acquire("192.0.2.1", "demo", "missing@example.test");
        limiter.failed(failed);
        AttemptTicket successful = limiter.acquire("192.0.2.1", "demo", "owner@example.test");
        limiter.succeeded(successful);

        assertEquals(0, successful.account().getFailures());
        assertEquals(1, successful.source().getFailures());
    }
}
