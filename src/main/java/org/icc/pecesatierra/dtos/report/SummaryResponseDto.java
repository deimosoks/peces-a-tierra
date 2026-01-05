package org.icc.pecesatierra.dtos.report;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResponseDto {

    private long totalAttendances;
    private int uniquePeople;

}
