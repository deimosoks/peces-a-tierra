package org.icc.pecesatierra.unit.attendance;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.icc.pecesatierra.features.attendance.Attendance;
import org.icc.pecesatierra.features.report.dtos.ReportRequestDto;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.specs.AttendanceReportSpecification;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * H-29: un {@code endDate} a medianoche perdia el resto del ultimo dia porque
 * el rango comparaba con {@code <=} contra esa medianoche. Ahora el limite
 * superior es exclusivo en el inicio del dia siguiente.
 */
@ExtendWith(MockitoExtension.class)
class AttendanceReportSpecificationTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Root<Attendance> root;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaQuery<?> query;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaBuilder cb;

    private final AttendanceReportSpecification specification =
            new AttendanceReportSpecification(new DateTimeUtils());

    private final User admin = TestFixtures.user("admin-1", "admin-a",
            TestFixtures.member("admin-member", TestFixtures.branch("branch-a", "Sede A"), "Admin A"),
            "ADMINISTRATOR");

    @Test
    void endDateAtMidnightCoversTheWholeLastDay_H29() {
        ReportRequestDto dto = ReportRequestDto.builder()
                .endDate(LocalDateTime.of(2026, 1, 15, 0, 0))
                .build();

        specification.build(dto, admin).toPredicate(root, query, cb);

        ArgumentCaptor<OffsetDateTime> upperExclusive = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(cb).lessThan(any(), upperExclusive.capture());

        assertThat(upperExclusive.getValue())
                .isEqualTo(OffsetDateTime.parse("2026-01-16T05:00:00Z"));

        // 23:59 del 15 de enero en Bogota entra; 00:00 del 16 no.
        assertThat(OffsetDateTime.parse("2026-01-16T04:59:59Z"))
                .isBefore(upperExclusive.getValue());
        assertThat(OffsetDateTime.parse("2026-01-16T05:00:00Z").isBefore(upperExclusive.getValue()))
                .isFalse();
    }

    @Test
    void startDateStillUsesMidnightOfTheRequestedDay_H29() {
        ReportRequestDto dto = ReportRequestDto.builder()
                .startDate(LocalDateTime.of(2026, 1, 15, 0, 0))
                .build();

        specification.build(dto, admin).toPredicate(root, query, cb);

        ArgumentCaptor<OffsetDateTime> lowerInclusive = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(cb).greaterThanOrEqualTo(any(), lowerInclusive.capture());

        assertThat(lowerInclusive.getValue())
                .isEqualTo(OffsetDateTime.parse("2026-01-15T05:00:00Z"));
    }
}
