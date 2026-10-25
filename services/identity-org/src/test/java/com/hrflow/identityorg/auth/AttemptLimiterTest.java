package com.hrflow.identityorg.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AttemptLimiterTest {
    @Test
    void fifthFailureBlocksAndSuccessClearsCounter() {
        AuthAttemptRepository repository = mock(AuthAttemptRepository.class);
        AuthAttempt row = new AuthAttempt();
        row.windowStarted = Instant.now();
        when(repository.findLocked(any())).thenReturn(Optional.of(row));
        AttemptLimiter limiter = new AttemptLimiter(repository);
        for (int index = 0; index < 5; index++) limiter.failed(limiter.acquire("demo", "user@example.test"));
        assertEquals(5, row.failures);
        assertThrows(AuthFailure.class, () -> limiter.acquire("demo", "user@example.test"));
        row.blockedUntil = Instant.now().minusSeconds(1);
        limiter.succeeded(limiter.acquire("demo", "user@example.test"));
        assertEquals(0, row.failures);
        assertNull(row.blockedUntil);
        verify(repository, times(7)).ensure(any(), any());
    }
}
