package org.icc.pecesatierra.features.service.dtos.event;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceEventResponseDto {

    private String id;
    private String serviceName;
    private String branchName;
    private String createdBy;
    private String address;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

}
