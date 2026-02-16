package org.icc.pecesatierra.web.services.impl;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.dashboard.DashboardResponseDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.dtos.report.ReportResponseDto;
import org.icc.pecesatierra.utils.mappers.MemberMapper;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.web.services.DashboardService;
import org.icc.pecesatierra.web.services.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class DashboardServiceImpl implements DashboardService {

        private final MemberRepository memberRepository;
        private final MemberMapper memberMapper;
        private final ReportService reportService;

        private final UserRepository userRepository;

        @Transactional(readOnly = true)
        @Override
        public DashboardResponseDto dashboard() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String username = auth.getName();

                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null) {
                        boolean isAdmin = user.hasAuthority("ADMINISTRATOR");

                        long totalMember;
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
                        } else {
                                org.icc.pecesatierra.entities.Branch branch = user.getMember().getBranch();
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
                                }
                        }

                        List<ReportResponseDto> lastWeekReport = reportService.generate(reportRequest);

                        return DashboardResponseDto.builder()
                                        .totalMember(totalMember)
                                        .membersBirthdays(memberBirthdays)
                                        .lastWeekReport(lastWeekReport)
                                        .build();
                }
                throw new EntityNotFoundException("User not found");
        }
}
