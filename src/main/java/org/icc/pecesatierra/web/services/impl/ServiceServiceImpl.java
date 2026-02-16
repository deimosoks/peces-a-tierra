package org.icc.pecesatierra.web.services.impl;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.service.ServiceRequestDto;
import org.icc.pecesatierra.dtos.service.ServiceResponseDto;
import org.icc.pecesatierra.entities.Services;
import org.icc.pecesatierra.exceptions.ServiceHasHistoricalRecordException;
import org.icc.pecesatierra.exceptions.ServicesNotFoundException;
import org.icc.pecesatierra.repositories.ServiceEventRepository;
import org.icc.pecesatierra.utils.mappers.ServiceMapper;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.ServiceRepository;
import org.icc.pecesatierra.web.services.ServiceEventService;
import org.icc.pecesatierra.web.services.ServiceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;
    private final AttendanceRepository attendanceRepository;
    private final ServiceEventRepository serviceEventRepository;

    @Transactional
    @Override
    public ServiceResponseDto create(ServiceRequestDto serviceRequestDto) {

        Services services = Services.builder()
                .name(serviceRequestDto.getName())
                .description(serviceRequestDto.getDescription())
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();

        return serviceMapper.toDto(serviceRepository.save(services));
    }

    @Transactional
    @Override
    public ServiceResponseDto update(ServiceRequestDto serviceRequestDto, String serviceId) {
        Services services = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServicesNotFoundException("Este servicio no existe."));

        serviceMapper.updateEntityFromDto(serviceRequestDto, services);

        services.setUpdatedAt(LocalDateTime.now());

        return serviceMapper.toDto(serviceRepository.save(services));
    }

    @Transactional
    @Override
    public void delete(String serviceId) {
        Services services = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServicesNotFoundException("Este servicio no existe."));

        if (serviceEventRepository.existsByServices(services))
            throw new ServiceHasHistoricalRecordException("Este servicio tiene registro histórico asi que no puede ser eliminado, considere desactivarlo.");

        serviceRepository.delete(services);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ServiceResponseDto> findAll(boolean onlyActive) {
        return onlyActive ?
                serviceRepository.findAllByActiveTrue().stream().map(serviceMapper::toDto).toList()
                :
                serviceRepository.findAll().stream().map(serviceMapper::toDto).toList();
    }

    @Transactional
    @Override
    public boolean updateActive(String serviceId, boolean active) {
        Services services = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServicesNotFoundException("Este servicio no existe."));

        services.setActive(active);

        serviceRepository.save(services);

        return services.isActive();
    }


}
