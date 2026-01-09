package org.icc.pecesatierra.services.impl;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.dashboard.DashboardResponseDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.dtos.report.ReportResponseDto;
import org.icc.pecesatierra.mappers.MemberMapper;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.services.DashboardService;
import org.icc.pecesatierra.services.ReportService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final ReportService reportService;

    @Override
    public DashboardResponseDto dashboard() {

        long totalMember = memberRepository.count();

        List<MemberResponseDto> memberBirthdays =
                memberRepository.findMembersWithBirthdayInMonth(LocalDateTime.now().getMonthValue())
                        .stream().map(memberMapper::toDto).toList();

        long lastServiceAssistance = attendanceRepository.countAttendanceLastService();

        List<ReportResponseDto> lastWeekReport = reportService.generate(
                ReportRequestDto.builder()
                        .startDate(LocalDateTime.now().minusDays(7))
                        .endDate(LocalDateTime.now())
                        .build()
        );

        return DashboardResponseDto.builder()
                .totalMember(totalMember)
                .membersBirthdays(memberBirthdays)
                .lastServiceAssistance(lastServiceAssistance)
                .lastWeekReport(lastWeekReport)
                .build();
    }
}
