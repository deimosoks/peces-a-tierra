package org.icc.pecesatierra.web.services.impl;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.service.ServiceRequestDto;
import org.icc.pecesatierra.dtos.service.ServiceResponseDto;
import org.icc.pecesatierra.domain.reference.Services;
import org.icc.pecesatierra.exceptions.ServiceHasHistoricalRecordException;
import org.icc.pecesatierra.exceptions.ServicesNotFoundException;
import org.icc.pecesatierra.helpers.mappers.ServiceMapper;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.ServiceRepository;
import org.icc.pecesatierra.web.services.ServiceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;
    private final AttendanceRepository attendanceRepository;

    @Override
    public ServiceResponseDto create(ServiceRequestDto serviceRequestDto) {

        Services services = Services.builder()
                .name(serviceRequestDto.getName())
                .description(serviceRequestDto.getDescription())
//                .dayOfWeek(serviceRequestDto.getDayOfWeek().toString())
//                .startTime(serviceRequestDto.getStartTime())
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();

        return serviceMapper.toDto(serviceRepository.save(services));
    }

    @Override
    public ServiceResponseDto update(ServiceRequestDto serviceRequestDto, String serviceId) {
        Services services = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServicesNotFoundException("Services doesn't exist."));

        serviceMapper.updateEntityFromDto(serviceRequestDto, services);

        services.setUpdatedAt(LocalDateTime.now());

        return serviceMapper.toDto(serviceRepository.save(services));
    }

    @Override
    public void delete(String serviceId) {
        Services services = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServicesNotFoundException("Services doesn't exist."));

        if (attendanceRepository.existsByServices(services))
            throw new ServiceHasHistoricalRecordException("Service have a historical record and cannot be deleted.");

        serviceRepository.delete(services);
    }

    @Override
    public List<ServiceResponseDto> findAll(boolean onlyActive) {
        return onlyActive ?
                serviceRepository.findAllByActiveTrue().stream().map(serviceMapper::toDto).toList()
                :
                serviceRepository.findAll().stream().map(serviceMapper::toDto).toList();
    }

    @Override
    public boolean updateActive(String serviceId, boolean active) {
        Services services = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServicesNotFoundException("Service doesn't exist."));

        services.setActive(active);

        serviceRepository.save(services);

        return services.isActive();
    }


}
