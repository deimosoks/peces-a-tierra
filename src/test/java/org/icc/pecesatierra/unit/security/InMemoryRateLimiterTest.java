package org.icc.pecesatierra.unit.security;

import org.icc.pecesatierra.utils.security.InMemoryRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * H-18: el limitador debe permitir N peticiones, bloquear la siguiente, reiniciar
 * la cuota al cambiar de ventana y no crecer sin limite.
 */
class InMemoryRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-25T12:00:00Z"));

    @Test
    void allowsTheConfiguredAmountAndRejectsTheNextOne_H18() {
        InMemoryRateLimiter limiter =
                new InMemoryRateLimiter(3, Duration.ofSeconds(60), 100, clock);

        assertThat(limiter.tryAcquire("login|1.1.1.1|ana")).isTrue();
        assertThat(limiter.tryAcquire("login|1.1.1.1|ana")).isTrue();
        assertThat(limiter.tryAcquire("login|1.1.1.1|ana")).isTrue();
        assertThat(limiter.tryAcquire("login|1.1.1.1|ana")).isFalse();
    }

    @Test
    void eachKeyHasItsOwnCounter_H18() {
        InMemoryRateLimiter limiter =
                new InMemoryRateLimiter(1, Duration.ofSeconds(60), 100, clock);

        assertThat(limiter.tryAcquire("login|1.1.1.1|ana")).isTrue();
        assertThat(limiter.tryAcquire("login|1.1.1.1|ana")).isFalse();
        assertThat(limiter.tryAcquire("login|1.1.1.1|beto")).isTrue();
        assertThat(limiter.tryAcquire("login|2.2.2.2|ana")).isTrue();
    }

    @Test
    void windowResetsAfterTheConfiguredTime_H18() {
        InMemoryRateLimiter limiter =
                new InMemoryRateLimiter(2, Duration.ofSeconds(60), 100, clock);

        assertThat(limiter.tryAcquire("ai|ana")).isTrue();
        assertThat(limiter.tryAcquire("ai|ana")).isTrue();
        assertThat(limiter.tryAcquire("ai|ana")).isFalse();

        clock.advance(Duration.ofSeconds(59));
        assertThat(limiter.tryAcquire("ai|ana")).isFalse();

        clock.advance(Duration.ofSeconds(1));
        assertThat(limiter.tryAcquire("ai|ana")).isTrue();
    }

    @Test
    void trackedKeysNeverGrowBeyondTheLimit_H18() {
        InMemoryRateLimiter limiter =
                new InMemoryRateLimiter(5, Duration.ofSeconds(60), 10, clock);

        for (int index = 0; index < 200; index++) {
            limiter.tryAcquire("login|10.0.0." + index + "|usuario" + index);
        }

        assertThat(limiter.trackedKeys()).isLessThanOrEqualTo(10);
    }

    @Test
    void expiredWindowsAreEvictedBeforeDroppingLiveOnes_H18() {
        InMemoryRateLimiter limiter =
                new InMemoryRateLimiter(1, Duration.ofSeconds(60), 2, clock);

        limiter.tryAcquire("login|1.1.1.1|ana");
        limiter.tryAcquire("login|1.1.1.1|beto");

        clock.advance(Duration.ofSeconds(61));
        limiter.tryAcquire("login|1.1.1.1|carla");

        // Las dos ventanas viejas estaban expiradas: no se descarta ninguna nueva.
        assertThat(limiter.trackedKeys()).isLessThanOrEqualTo(2);
        assertThat(limiter.tryAcquire("login|1.1.1.1|carla")).isFalse();
    }

    @Test
    void rejectsInvalidConfiguration_H18() {
        assertThatThrownBy(() -> new InMemoryRateLimiter(0, Duration.ofSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InMemoryRateLimiter(1, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Reloj de prueba que solo avanza cuando el test lo pide. */
    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
