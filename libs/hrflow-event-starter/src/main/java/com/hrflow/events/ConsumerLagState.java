package com.hrflow.events;

import java.time.Duration;
import java.time.Instant;

/** Unknown/stale measurements are distinct from a confirmed caught-up broker. */
public final class ConsumerLagState {
    private long lag = -1;
    private Instant measured, highSince;
    private boolean alerted;
    public enum Change { NONE, ALERT, RECOVERED }
    public synchronized Change observe(long value, Instant now) {
        if (value < 0) throw new IllegalArgumentException("Broker lag cannot be negative");
        lag = value; measured = now;
        if (value > 100) {
            if (highSince == null) highSince = now;
            if (!alerted && !now.isBefore(highSince.plusSeconds(300))) { alerted = true; return Change.ALERT; }
        } else {
            highSince = null;
            if (alerted) { alerted = false; return Change.RECOVERED; }
        }
        return Change.NONE;
    }
    public synchronized void unavailable() { lag = -1; measured = null; highSince = null; }
    public synchronized long lag(Instant now) {
        return measured == null || Duration.between(measured, now).toSeconds() > 30 ? -1 : lag;
    }
}
