package org.icc.pecesatierra.dtos.report;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDto {

    private LocalDate date;
    private OffsetDateTime serviceTime;

    private String serviceName;

    private String category;
    private String typePeople;
    private String subCategory;

    private String branchName;

    private Long total;
}
