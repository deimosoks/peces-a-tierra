package org.icc.pecesatierra.dtos.dashboard;

import lombok.*;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.dtos.report.ReportResponseDto;

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

}
