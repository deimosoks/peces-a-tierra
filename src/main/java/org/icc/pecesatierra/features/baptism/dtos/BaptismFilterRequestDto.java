package org.icc.pecesatierra.features.baptism.dtos;

import lombok.*;
import org.icc.pecesatierra.utils.models.OrderBy;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaptismFilterRequestDto {

    private String memberId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String query;
    private boolean active;
    private String branchId;
    private OrderBy orderBy;
}
