package org.icc.pecesatierra.dtos.baptism;

import lombok.*;

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

}
