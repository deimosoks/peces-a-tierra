package org.icc.pecesatierra.dtos.service.event;

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
