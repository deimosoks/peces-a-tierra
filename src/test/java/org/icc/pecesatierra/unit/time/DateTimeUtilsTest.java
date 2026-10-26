package org.icc.pecesatierra.unit.time;

import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilsTest {

    private final DateTimeUtils dateTimeUtils = new DateTimeUtils();

    @Test
    void convertsColombiaLocalTimeToUtcCrossingMidnight() {
        // 23:30 en Bogota del 14 de enero son las 04:30Z del 15 de enero.
        OffsetDateTime utc = dateTimeUtils.toUTC(LocalDateTime.of(2026, 1, 14, 23, 30));

        assertThat(utc).isEqualTo(OffsetDateTime.of(2026, 1, 15, 4, 30, 0, 0, ZoneOffset.UTC));
        assertThat(utc.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void convertsUtcToColombiaPreviousDay() {
        // 02:00Z del 15 de enero son las 21:00 del 14 de enero en Bogota.
        LocalDateTime colombia = dateTimeUtils.toColombia(OffsetDateTime.parse("2026-01-15T02:00:00Z"));

        assertThat(colombia).isEqualTo(LocalDateTime.of(2026, 1, 14, 21, 0));
    }

    @Test
    void roundTripKeepsTheSameInstant() {
        LocalDateTime original = LocalDateTime.of(2026, 6, 15, 10, 15, 30);

        assertThat(dateTimeUtils.toColombia(dateTimeUtils.toUTC(original))).isEqualTo(original);
    }

    @Test
    void nullsArePropagated() {
        assertThat(dateTimeUtils.toUTC(null)).isNull();
        assertThat(dateTimeUtils.toColombia(null)).isNull();
        assertThat(dateTimeUtils.startOfNextDayUTC(null)).isNull();
        assertThat(dateTimeUtils.startOfNextDay(null)).isNull();
    }

    @Test
    void startOfNextDayUtcMakesTheWholeLastDayInclusive_H29() {
        // endDate a medianoche del 15 de enero en Bogota: el limite exclusivo
        // es la medianoche del 16, es decir 05:00Z del 16.
        OffsetDateTime upperExclusive =
                dateTimeUtils.startOfNextDayUTC(LocalDateTime.of(2026, 1, 15, 0, 0));

        assertThat(upperExclusive).isEqualTo(OffsetDateTime.parse("2026-01-16T05:00:00Z"));

        // 23:59 del 15 de enero en Colombia entra; 00:00 del 16 ya no.
        assertThat(OffsetDateTime.parse("2026-01-16T04:59:59Z")).isBefore(upperExclusive);
        assertThat(OffsetDateTime.parse("2026-01-16T05:00:00Z").isBefore(upperExclusive)).isFalse();
    }

    @Test
    void startOfNextDayForLocalDateColumns_H29() {
        assertThat(dateTimeUtils.startOfNextDay(LocalDate.of(2026, 1, 15)))
                .isEqualTo(LocalDate.of(2026, 1, 16));
    }

    @Test
    void nowUtcUsesUtcOffset() {
        assertThat(dateTimeUtils.nowUTC().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void nowColombiaUsesBusinessZone() {
        LocalDateTime colombia = dateTimeUtils.nowColombia();
        LocalDateTime utcAsColombia = dateTimeUtils.toColombia(dateTimeUtils.nowUTC());

        // Ambas lecturas usan el mismo reloj del sistema; se permite una diferencia
        // de un segundo para no depender del instante exacto de ejecucion.
        assertThat(Math.abs(Duration.between(colombia, utcAsColombia).toMillis()))
                .isLessThan(1_000L);
    }
}
