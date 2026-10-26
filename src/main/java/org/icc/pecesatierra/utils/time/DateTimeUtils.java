package org.icc.pecesatierra.utils.time;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.icc.pecesatierra.utils.time.TimeConstants.COLOMBIA_ZONE;

@Component
public class DateTimeUtils {

    public OffsetDateTime toUTC(LocalDateTime colombiaTime) {
        return colombiaTime != null ? colombiaTime.atZone(COLOMBIA_ZONE)
                .toOffsetDateTime()
                .withOffsetSameInstant(ZoneOffset.UTC) : null;
    }

    public LocalDateTime toColombia(OffsetDateTime utcTime) {
        return utcTime != null ? utcTime.atZoneSameInstant(COLOMBIA_ZONE).toLocalDateTime() : null;
    }

    /**
     * Limite superior EXCLUSIVO para un rango de fechas: el inicio del dia
     * siguiente al recibido, en hora de Colombia. Un {@code endDate} a
     * medianoche sigue incluyendo todo ese dia (H-29).
     */
    public OffsetDateTime startOfNextDayUTC(LocalDateTime colombiaTime) {
        return colombiaTime != null
                ? toUTC(colombiaTime.toLocalDate().plusDays(1).atStartOfDay())
                : null;
    }

    /**
     * Equivalente a {@link #startOfNextDayUTC(LocalDateTime)} para columnas
     * {@link LocalDate}: el dia siguiente, que actua como limite exclusivo.
     */
    public LocalDate startOfNextDay(LocalDate date) {
        return date != null ? date.plusDays(1) : null;
    }

    public LocalDateTime nowColombia() {
        return LocalDateTime.now(TimeConstants.COLOMBIA_ZONE);
    }

    public OffsetDateTime nowUTC() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

}
