package org.icc.pecesatierra.services;

import org.icc.pecesatierra.dtos.service.ServiceRequestDto;
import org.icc.pecesatierra.dtos.service.ServiceResponseDto;

import java.util.List;

public interface ServiceService {
    ServiceResponseDto create(ServiceRequestDto serviceRequestDto);

    ServiceResponseDto update(ServiceRequestDto serviceRequestDto, String serviceId);

    void delete(String serviceId);

    List<ServiceResponseDto> findAll(boolean onlyActive);

    boolean updateActive(String serviceId, boolean active);
}
