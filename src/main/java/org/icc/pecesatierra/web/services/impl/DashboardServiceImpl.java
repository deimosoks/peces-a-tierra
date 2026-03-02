package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.dashboard.DashboardResponseDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.dtos.report.ReportResponseDto;
import org.icc.pecesatierra.entities.Branch;
import org.icc.pecesatierra.repositories.BaptismRepository;
import org.icc.pecesatierra.utils.mappers.MemberMapper;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.web.services.DashboardService;
import org.icc.pecesatierra.web.services.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.icc.pecesatierra.entities.User;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final ReportService reportService;
    private final BaptismRepository baptismRepository;

    @Transactional(readOnly = true)
    @Override
    public DashboardResponseDto dashboard(User user) {

        boolean isAdmin = user.hasAuthority("ADMINISTRATOR");

        long totalMember;
        long totalBaptisms = 0;
        List<MemberResponseDto> memberBirthdays;
        ReportRequestDto reportRequest = ReportRequestDto.builder()
                .startDate(LocalDateTime.now().minusDays(7))
                .endDate(LocalDateTime.now())
                .build();

        if (isAdmin) {
            totalMember = memberRepository.count();
            memberBirthdays = memberRepository
                    .findMembersWithBirthdayInMonth(LocalDateTime.now().getMonthValue())
                    .stream().map(member -> memberMapper.toDto(member, false)).toList();
            totalBaptisms = baptismRepository.countByInvalidFalse();
        } else {
            Branch branch = user.getMember().getBranch();
            if (branch == null) {
                totalMember = 0;
                memberBirthdays = List.of();
                reportRequest.setBranchIds(List.of("NON_EXISTENT_BRANCH"));
            } else {
                totalMember = memberRepository.countByBranch(branch);
                memberBirthdays = memberRepository.findMembersWithBirthdayInMonthAndBranch(
                                LocalDateTime.now().getMonthValue(), branch)
                        .stream().map(member -> memberMapper.toDto(member, false))
                        .toList();

                reportRequest.setBranchIds(List.of(branch.getId()));
                totalBaptisms = baptismRepository.countByBaptizedMemberBranchAndInvalidFalse(branch);
            }
        }

        List<ReportResponseDto> lastWeekReport = reportService.generate(reportRequest, user);

        return DashboardResponseDto.builder()
                .totalMember(totalMember)
                .membersBirthdays(memberBirthdays)
                .lastWeekReport(lastWeekReport)
                .totalBaptisms(totalBaptisms)
                .build();
    }
}
