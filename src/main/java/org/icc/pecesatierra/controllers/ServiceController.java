package org.icc.pecesatierra.controllers;

import org.icc.pecesatierra.dtos.service.ServiceRequestDto;
import org.icc.pecesatierra.dtos.service.ServiceResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ServiceController {
    ResponseEntity<ServiceResponseDto> create(ServiceRequestDto serviceRequestDto);
    ResponseEntity<ServiceResponseDto> update(ServiceRequestDto serviceRequestDto, String serviceId);
    ResponseEntity<ServiceResponseDto> delete(String serviceId);
    ResponseEntity<List<ServiceResponseDto>> findAll(boolean onlyActive);
    ResponseEntity<Boolean> updateState(String serviceId, boolean state);
}
