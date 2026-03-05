package org.icc.pecesatierra.utils.mappers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.service.event.ServiceEventResponseDto;
import org.icc.pecesatierra.entities.ServiceEvent;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceEventMapper {

    private final DateTimeUtils dateTimeUtils;

    public ServiceEventResponseDto toDto(ServiceEvent serviceEvent) {

        return ServiceEventResponseDto.builder()
                .id(serviceEvent.getId())
                .serviceName(serviceEvent.getServices().getName())
                .branchName(serviceEvent.getBranch().getName())
                .createdBy(serviceEvent.getCreatedBy().getCompleteName())
                .address(serviceEvent.getBranch().getAddress())
                .startDateTime(dateTimeUtils.toColombia(serviceEvent.getStartDateTime()))
                .endDateTime(dateTimeUtils.toColombia(serviceEvent.getEndDateTime()))
                .build();

    }

}
