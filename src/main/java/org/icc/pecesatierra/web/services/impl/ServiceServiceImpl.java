package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.service.ServiceRequestDto;
import org.icc.pecesatierra.dtos.service.ServiceResponseDto;
import org.icc.pecesatierra.entities.Services;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.services.CannotDeleteServiceWithRecords;
import org.icc.pecesatierra.exceptions.services.ServicesNotFoundException;
import org.icc.pecesatierra.repositories.ServiceEventRepository;
import org.icc.pecesatierra.utils.mappers.ServiceMapper;
import org.icc.pecesatierra.repositories.ServiceRepository;
import org.icc.pecesatierra.web.services.ServiceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;
    private final ServiceEventRepository serviceEventRepository;

    @Transactional
    @Override
    public ServiceResponseDto create(ServiceRequestDto serviceRequestDto, User user) {

        Services services = Services.builder()
                .name(serviceRequestDto.getName())
                .description(serviceRequestDto.getDescription())
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();

        serviceRepository.save(services);

        log.info("""
                        Usuario {} creó un servicio:
                        ID: {}
                        Nombre: {}
                        Descripción: {}
                        Activo: {}
                        """,
                user.getMember().getId(),
                services.getId(),
                services.getName(),
                services.getDescription(),
                services.isActive()
        );

        return serviceMapper.toDto(services);
    }

    @Transactional
    @Override
    public ServiceResponseDto update(ServiceRequestDto serviceRequestDto, String serviceId, User user) {
        Services services = serviceRepository.findById(serviceId)
                .orElseThrow(ServicesNotFoundException::new);

        Services beforeUpdate = Services.builder()
                .id(services.getId())
                .name(services.getName())
                .description(services.getDescription())
                .active(services.isActive())
                .createdAt(services.getCreatedAt())
                .updatedAt(services.getUpdatedAt())
                .build();

        serviceMapper.updateEntityFromDto(serviceRequestDto, services);

        services.setUpdatedAt(LocalDateTime.now());

        serviceRepository.save(services);

        log.info("""
                        Usuario {} actualizó un servicio.
                        Estado anterior:
                        ID: {}
                        Nombre: {}
                        Descripción: {}
                        Activo: {}
                        Estado actualizado:
                        ID: {}
                        Nombre: {}
                        Descripción: {}
                        Activo: {}
                        """,
                user.getMember().getId(),
                beforeUpdate.getId(),
                beforeUpdate.getName(),
                beforeUpdate.getDescription(),
                beforeUpdate.isActive(),
                services.getId(),
                services.getName(),
                services.getDescription(),
                services.isActive()
        );

        return serviceMapper.toDto(services);
    }

    @Transactional
    @Override
    public void delete(String serviceId, User user) {
        Services services = serviceRepository.findById(serviceId)
                .orElseThrow(ServicesNotFoundException::new);

        if (serviceEventRepository.existsByServices(services)) {
            log.warn("Usuario {} intentó eliminar servicio {} pero tiene registros históricos",
                    user.getMember().getId(), services.getId());
            throw new CannotDeleteServiceWithRecords(services.getName());
        }

        log.info("""
                        Usuario {} eliminó un servicio:
                        ID: {}
                        Nombre: {}
                        Descripción: {}
                        """,
                user.getMember().getId(),
                services.getId(),
                services.getName(),
                services.getDescription()
        );

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
    public boolean updateActive(String serviceId, boolean active, User user) {
        Services services = serviceRepository.findById(serviceId)
                .orElseThrow(ServicesNotFoundException::new);

        services.setActive(active);

        serviceRepository.save(services);

        log.info("""
                        Usuario {} cambió el estado activo de un servicio:
                        ID: {}
                        Nombre: {}
                        Nuevo estado: {}
                        """,
                user.getMember().getId(),
                services.getId(),
                services.getName(),
                services.isActive()
        );

        return services.isActive();
    }


}
