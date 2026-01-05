package org.icc.pecesatierra.dtos.report;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryDto {
    private String dato;
    private long total;
}
