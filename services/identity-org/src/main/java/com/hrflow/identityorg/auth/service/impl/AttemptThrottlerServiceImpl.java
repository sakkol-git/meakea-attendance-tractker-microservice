package com.hrflow.identityorg.auth.service.impl;

import com.hrflow.identityorg.auth.entity.AuthAttempt;
import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.auth.repository.AuthAttemptRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import com.hrflow.identityorg.auth.service.AttemptThrottlerService;
import com.hrflow.identityorg.auth.service.record.AttemptTicket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttemptThrottlerServiceImpl implements AttemptThrottlerService {
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final AuthAttemptRepository repository;
    private final Clock clock;
    private final int accountThreshold;
    private final int sourceThreshold;

    public AttemptThrottlerServiceImpl(AuthAttemptRepository repository, Clock clock,
                                       @Value("${hrflow.auth.account-attempt-limit:5}") int accountThreshold,
                                       @Value("${hrflow.auth.source-attempt-limit:50}") int sourceThreshold) {
        this.repository = repository;
        this.clock = clock;
        this.accountThreshold = accountThreshold;
        this.sourceThreshold = sourceThreshold;
    }

    @Override
    @Transactional(noRollbackFor = AuthFailedException.class)
    public AttemptTicket acquire(String sourceAddress, String tenant, String identifier) {
        Instant now = clock.instant();
        String accountId = hash("account:" + normalize(tenant) + ":" + normalize(identifier));
        String sourceId = hash("source:" + normalize(sourceAddress));
        List<String> ids = java.util.stream.Stream.of(accountId, sourceId).sorted().toList();
        ids.forEach(id -> repository.ensure(id, now));
        java.util.Map<String, AuthAttempt> locked = new java.util.HashMap<>();
        ids.forEach(id -> locked.put(id, repository.findLocked(id).orElseThrow()));
        ensureAllowed(locked.get(accountId), now);
        ensureAllowed(locked.get(sourceId), now);
        return new AttemptTicket(locked.get(accountId), locked.get(sourceId));
    }

    @Override
    @Transactional(noRollbackFor = AuthFailedException.class)
    public void failed(AttemptTicket ticket) {
        Instant now = clock.instant();
        ticket.account().recordFailure(now, accountThreshold, WINDOW);
        ticket.source().recordFailure(now, sourceThreshold, WINDOW);
        repository.saveAll(List.of(ticket.account(), ticket.source()));
        if (ticket.account().getBlockedUntil() != null || ticket.source().getBlockedUntil() != null) {
            throw throttled(ticket, now);
        }
    }

    @Override
    @Transactional
    public void succeeded(AttemptTicket ticket) {
        Instant now = clock.instant();
        ticket.account().reset(now);
        ticket.source().reset(now);
        repository.saveAll(List.of(ticket.account(), ticket.source()));
    }

    private void ensureAllowed(AuthAttempt attempt, Instant now) {
        if (attempt.getBlockedUntil() != null && attempt.getBlockedUntil().isAfter(now)) {
            throw throttled(attempt.getBlockedUntil(), now);
        }
        if (attempt.getWindowStarted() == null || !attempt.getWindowStarted().plus(WINDOW).isAfter(now)) {
            attempt.reset(now);
        }
    }

    private AuthFailedException throttled(AttemptTicket ticket, Instant now) {
        Instant until = java.util.stream.Stream.of(
                ticket.account().getBlockedUntil(), ticket.source().getBlockedUntil())
                .filter(java.util.Objects::nonNull).max(Instant::compareTo).orElse(now.plus(WINDOW));
        return throttled(until, now);
    }

    private AuthFailedException throttled(Instant until, Instant now) {
        long retryAfter = Math.max(1, Duration.between(now, until).toSeconds());
        return new AuthFailedException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS",
                "Too many failed attempts. Try again later.", retryAfter);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
