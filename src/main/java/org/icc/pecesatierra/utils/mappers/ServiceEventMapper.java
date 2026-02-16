package org.icc.pecesatierra.utils.mappers;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.service.event.ServiceEventResponseDto;
import org.icc.pecesatierra.entities.ServiceEvent;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ServiceEventMapper {

    public ServiceEventResponseDto toDto(ServiceEvent serviceEvent){

        return ServiceEventResponseDto.builder()
                .id(serviceEvent.getId())
                .serviceName(serviceEvent.getServices().getName())
                .branchName(serviceEvent.getBranch().getName())
                .createdBy(serviceEvent.getCreatedBy().getCompleteName())
                .address(serviceEvent.getBranch().getAddress())
                .startDateTime(serviceEvent.getStartDateTime())
                .endDateTime(serviceEvent.getEndDateTime())
                .build();

    }

}
