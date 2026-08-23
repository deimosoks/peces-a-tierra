package org.icc.pecesatierra.features.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.dashboard.dtos.DashboardResponseDto;
import org.icc.pecesatierra.features.member.dtos.MemberResponseDto;
import org.icc.pecesatierra.features.report.dtos.ReportRequestDto;
import org.icc.pecesatierra.features.report.dtos.ReportResponseDto;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.baptism.repository.BaptismRepository;
import org.icc.pecesatierra.features.category.repository.mapper.MemberMapper;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.report.service.ReportService;
import org.icc.pecesatierra.utils.enums.AppPermission;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.icc.pecesatierra.features.user.User;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final ReportService reportService;
    private final BaptismRepository baptismRepository;
    private final DateTimeUtils dateTimeUtils;

    @Transactional(readOnly = true)
    public DashboardResponseDto dashboard(User user) {

        boolean isAdmin = user.hasAuthority(AppPermission.ADMINISTRATOR.name());

        long totalMember;
        long totalBaptisms = 0;
        List<MemberResponseDto> memberBirthdays;
        ReportRequestDto reportRequest = ReportRequestDto.builder()
                .startDate(LocalDateTime.now().minusDays(7))
                .endDate(LocalDateTime.now())
                .build();


        List<MemberResponseDto> latestMemberRegisteredInThLastMonth = new ArrayList<>();

        if (isAdmin) {
            totalMember = memberRepository.count();
            memberBirthdays = memberRepository
                    .findMembersWithBirthdayInMonth(dateTimeUtils.nowColombia().getMonthValue())
                    .stream().map(member -> memberMapper.toDto(member, false)).toList();
            totalBaptisms = baptismRepository.countByInvalidFalse();

            latestMemberRegisteredInThLastMonth = memberRepository.findLatestMembers(OffsetDateTime.now().minusDays(30)).stream().map(member -> memberMapper.toDto(member, false)).toList();
        } else {
            Branch branch = user.getMember().getBranch();

            totalMember = memberRepository.countByBranch(branch);
            memberBirthdays = memberRepository.findMembersWithBirthdayInMonthAndBranch(
                            dateTimeUtils.nowColombia().getMonthValue(), branch)
                    .stream().map(member -> memberMapper.toDto(member, false))
                    .toList();

            reportRequest.setBranchIds(List.of(branch.getId()));
            totalBaptisms = baptismRepository.countByBaptizedMemberBranchAndInvalidFalse(branch);

            latestMemberRegisteredInThLastMonth = memberRepository.findLatestMembersByBranch(OffsetDateTime.now().minusDays(30), branch.getId()).stream().map(member -> memberMapper.toDto(member, false)).toList();

        }

        List<ReportResponseDto> lastWeekReport = reportService.generate(reportRequest, user);

        return DashboardResponseDto.builder()
                .totalMember(totalMember)
                .membersBirthdays(memberBirthdays)
                .lastWeekReport(lastWeekReport)
                .totalBaptisms(totalBaptisms)
                .latestMemberRegisteredInThLastMonth(latestMemberRegisteredInThLastMonth)
                .build();
    }
}
