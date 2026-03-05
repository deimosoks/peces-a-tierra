package org.icc.pecesatierra.utils.time;

import org.springframework.stereotype.Component;

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

    public LocalDateTime nowColombia() {
        return LocalDateTime.now(TimeConstants.COLOMBIA_ZONE);
    }

    public OffsetDateTime nowUTC() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

}
