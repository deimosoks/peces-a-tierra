package org.icc.pecesatierra.features.service.dtos.event;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceEventPagesResponseDto {

    private List<ServiceEventResponseDto> events;
    private int pages;

}
