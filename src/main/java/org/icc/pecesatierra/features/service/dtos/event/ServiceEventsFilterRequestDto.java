package org.icc.pecesatierra.features.service.dtos.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceEventsFilterRequestDto {

    private String serviceId;
    private String branchId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

}
