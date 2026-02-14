package org.icc.pecesatierra.dtos.report;

import lombok.*;
import org.icc.pecesatierra.dtos.member.MemberCategoryResponseDto;
import org.icc.pecesatierra.dtos.member.MemberTypeResponseDto;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDto {

    private LocalDate date;
    private LocalDateTime serviceTime;

    private String serviceName;

    private String category;
    private String typePeople;
    private String subCategory;

    private Long total;
}



