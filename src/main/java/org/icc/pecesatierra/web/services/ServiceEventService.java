package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.service.event.ServiceEventRequestDto;
import org.icc.pecesatierra.dtos.service.event.ServiceEventResponseDto;
import org.icc.pecesatierra.dtos.service.event.ServiceEventsFilterRequestDto;
import org.icc.pecesatierra.dtos.service.event.ServiceEventPagesResponseDto;
import org.icc.pecesatierra.entities.ServiceEvent;
import org.icc.pecesatierra.entities.User;

import java.util.List;

public interface ServiceEventService {
    ServiceEventResponseDto create(ServiceEventRequestDto serviceEventRequestDto, User user);
    void cancel(String serviceEventId, User user);
    List<ServiceEventResponseDto> findForCalendar(ServiceEventsFilterRequestDto dto, User user);
    List<ServiceEventResponseDto> getActiveEventForUser(User user);
}
