package org.icc.pecesatierra.unit.dashboard;

import org.icc.pecesatierra.features.baptism.repository.BaptismRepository;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.category.repository.mapper.MemberMapper;
import org.icc.pecesatierra.features.dashboard.dtos.DashboardResponseDto;
import org.icc.pecesatierra.features.dashboard.service.DashboardService;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.report.dtos.ReportRequestDto;
import org.icc.pecesatierra.features.report.service.ReportService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * H-40: la ventana de 7 dias y la de ultimos 30 dias se calculan con la fecha
 * de America/Bogota. Antes se usaba el reloj del servidor, que en las primeras
 * horas del dia colombiano ya estaba en el dia siguiente.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberMapper memberMapper;
    @Mock
    private ReportService reportService;
    @Mock
    private BaptismRepository baptismRepository;
    @Mock
    private DateTimeUtils dateTimeUtils;

    @InjectMocks
    private DashboardService dashboardService;

    private final Branch branch = TestFixtures.branch("branch-a", "Sede A");

    @Test
    void weeklyWindowAndLastMonthStartUseColombiaNow_H40() {
        User admin = TestFixtures.user("admin-1", "admin-a",
                TestFixtures.member("admin-member", branch, "Admin A"), "ADMINISTRATOR");

        LocalDateTime nowColombia = LocalDateTime.of(2026, 3, 2, 0, 30);
        LocalDateTime lastMonthColombia = nowColombia.minusDays(30);
        OffsetDateTime lastMonthUTC = OffsetDateTime.parse("2026-01-31T05:30:00Z");

        when(dateTimeUtils.nowColombia()).thenReturn(nowColombia);
        when(dateTimeUtils.toUTC(lastMonthColombia)).thenReturn(lastMonthUTC);
        when(memberRepository.count()).thenReturn(10L);
        when(memberRepository.findMembersWithBirthdayInMonth(3)).thenReturn(List.of());
        when(baptismRepository.countByInvalidFalse()).thenReturn(2L);
        when(memberRepository.findLatestMembers(lastMonthUTC)).thenReturn(List.of());
        when(reportService.generate(any(ReportRequestDto.class), any(User.class))).thenReturn(List.of());

        DashboardResponseDto response = dashboardService.dashboard(admin);

        ArgumentCaptor<ReportRequestDto> captor = ArgumentCaptor.forClass(ReportRequestDto.class);
        verify(reportService).generate(captor.capture(), any(User.class));

        assertThat(captor.getValue().getEndDate()).isEqualTo(nowColombia);
        assertThat(captor.getValue().getStartDate()).isEqualTo(nowColombia.minusDays(7));
        verify(memberRepository).findLatestMembers(lastMonthUTC);
        assertThat(response.getTotalMember()).isEqualTo(10L);
        assertThat(response.getTotalBaptisms()).isEqualTo(2L);
    }

    @Test
    void branchDashboardAlsoUsesTheColombiaWindow_H40() {
        User operator = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-member", branch, "Operador A"));

        LocalDateTime nowColombia = LocalDateTime.of(2026, 3, 1, 23, 30);
        LocalDateTime lastMonthColombia = nowColombia.minusDays(30);
        OffsetDateTime lastMonthUTC = OffsetDateTime.parse("2026-01-30T04:30:00Z");

        when(dateTimeUtils.nowColombia()).thenReturn(nowColombia);
        when(dateTimeUtils.toUTC(lastMonthColombia)).thenReturn(lastMonthUTC);
        when(memberRepository.countByBranch(branch)).thenReturn(4L);
        when(memberRepository.findMembersWithBirthdayInMonthAndBranch(3, branch)).thenReturn(List.<Member>of());
        when(baptismRepository.countByBaptizedMemberBranchAndInvalidFalse(branch)).thenReturn(1L);
        when(memberRepository.findLatestMembersByBranch(lastMonthUTC, branch.getId())).thenReturn(List.<Member>of());
        when(reportService.generate(any(ReportRequestDto.class), any(User.class))).thenReturn(List.of());

        DashboardResponseDto response = dashboardService.dashboard(operator);

        ArgumentCaptor<ReportRequestDto> captor = ArgumentCaptor.forClass(ReportRequestDto.class);
        verify(reportService).generate(captor.capture(), any(User.class));

        assertThat(captor.getValue().getEndDate()).isEqualTo(nowColombia);
        assertThat(captor.getValue().getBranchIds()).containsExactly(branch.getId());
        verify(memberRepository).findLatestMembersByBranch(lastMonthUTC, branch.getId());
        assertThat(response.getTotalMember()).isEqualTo(4L);
    }
}
