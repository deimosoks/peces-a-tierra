package org.icc.pecesatierra.dtos.report;

import lombok.*;

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
    private List<String> subCategories;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String eventId;
    private String userId;
    private boolean onlyActive;

    private List<String> branchIds;
    private List<String> groupBy;

}
