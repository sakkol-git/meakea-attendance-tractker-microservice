package com.hrflow.events;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConsumerLagStateTest {
    @Test void unknownIsNeverZeroAndSustainedLagAlertsThenRecovers() {
        var state = new ConsumerLagState(); var now = Instant.parse("2026-10-29T00:00:00Z");
        assertThat(state.lag(now)).isEqualTo(-1);
        assertThat(state.observe(200, now)).isEqualTo(ConsumerLagState.Change.NONE);
        assertThat(state.observe(200, now.plusSeconds(299))).isEqualTo(ConsumerLagState.Change.NONE);
        assertThat(state.observe(200, now.plusSeconds(300))).isEqualTo(ConsumerLagState.Change.ALERT);
        assertThat(state.observe(200, now.plusSeconds(301))).isEqualTo(ConsumerLagState.Change.NONE);
        assertThat(state.observe(0, now.plusSeconds(302))).isEqualTo(ConsumerLagState.Change.RECOVERED);
        assertThat(state.lag(now.plusSeconds(302))).isZero();
        state.unavailable(); assertThat(state.lag(now.plusSeconds(303))).isEqualTo(-1);
        state.observe(0, now.plusSeconds(304)); assertThat(state.lag(now.plusSeconds(340))).isEqualTo(-1);
    }
}
