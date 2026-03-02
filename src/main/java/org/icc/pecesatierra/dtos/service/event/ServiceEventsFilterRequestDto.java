package org.icc.pecesatierra.dtos.service.event;

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
