package org.icc.pecesatierra.dtos.report;

import lombok.*;

import java.util.List;
import java.util.Map;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDto {

    private ReportRequestDto reportRequest;
    private SummaryResponseDto summary;
    private List<Map<String, ?>> reports;

}
