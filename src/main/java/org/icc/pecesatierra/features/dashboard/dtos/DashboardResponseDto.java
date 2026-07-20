package org.icc.pecesatierra.features.dashboard.dtos;

import lombok.*;
import org.icc.pecesatierra.features.member.dtos.MemberResponseDto;
import org.icc.pecesatierra.features.report.dtos.ReportResponseDto;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponseDto {

    private long totalMember;
    private List<MemberResponseDto> membersBirthdays;
    private long totalBaptisms;
    private List<ReportResponseDto> lastWeekReport;
    private List<MemberResponseDto> latestMemberRegisteredInThLastMonth;

}
