package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.service.ServiceRequestDto;
import org.icc.pecesatierra.dtos.service.ServiceResponseDto;
import org.icc.pecesatierra.entities.User;

import java.util.List;

public interface ServiceService {
    ServiceResponseDto create(ServiceRequestDto serviceRequestDto, User user);

    ServiceResponseDto update(ServiceRequestDto serviceRequestDto, String serviceId, User user);

    void delete(String serviceId, User user);

    List<ServiceResponseDto> findAll(boolean onlyActive);

    boolean updateActive(String serviceId, boolean active, User user);
}
