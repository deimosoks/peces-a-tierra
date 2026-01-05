package org.icc.pecesatierra.dtos.report;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDto {

    private List<String> typePeoples;
    private List<String> categories;
    private List<String> serviceIds;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String userId;
    private boolean onlyActive;

}
