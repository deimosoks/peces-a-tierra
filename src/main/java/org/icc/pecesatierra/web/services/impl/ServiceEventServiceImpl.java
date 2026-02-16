package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.service.event.ServiceEventRequestDto;
import org.icc.pecesatierra.dtos.service.event.ServiceEventResponseDto;
import org.icc.pecesatierra.entities.Branch;
import org.icc.pecesatierra.entities.ServiceEvent;
import org.icc.pecesatierra.entities.Services;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.*;
import org.icc.pecesatierra.repositories.BranchRepository;
import org.icc.pecesatierra.repositories.ServiceEventRepository;
import org.icc.pecesatierra.repositories.ServiceRepository;
import org.icc.pecesatierra.utils.mappers.ServiceEventMapper;
import org.icc.pecesatierra.web.services.ServiceEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceEventServiceImpl implements ServiceEventService {

    private final ServiceEventRepository serviceEventRepository;
    private final ServiceEventMapper serviceEventMapper;
    private final ServiceRepository serviceRepository;
    private final BranchRepository branchRepository;

    @Transactional
    @Override
    public ServiceEventResponseDto create(ServiceEventRequestDto serviceEventRequestDto, User user) {

        if (serviceEventRequestDto.getStartDateTime().isAfter(serviceEventRequestDto.getEndDateTime())) {
            throw new InvalidDatesException("La fecha de inicio no puede ser después de la fecha de finalización.");
        }

        Branch branch = branchRepository.findById(serviceEventRequestDto.getBranchId())
                .orElseThrow(() -> new BranchNotFoundException("Sede no encontrada."));

        Services service = serviceRepository.findById(serviceEventRequestDto.getServiceId())
                .orElseThrow(() -> new ServicesNotFoundException("Servicio no encontrado."));

        ServiceEvent serviceEvent = ServiceEvent.builder()
                .createdBy(user.getMember())
                .branch(branch)
                .services(service)
                .startDateTime(serviceEventRequestDto.getStartDateTime())
                .endDateTime(serviceEventRequestDto.getEndDateTime())
                .build();

        return serviceEventMapper.toDto(serviceEventRepository.save(serviceEvent));
    }

    @Transactional
    @Override
    public void cancel(String serviceEventId) {
        ServiceEvent serviceEvent = serviceEventRepository.findById(serviceEventId)
                .orElseThrow(() -> new ServiceEventNotFoundException("Evento no encontrado."));

        if (serviceEvent.getStartDateTime().isBefore(LocalDateTime.now())) {
            throw new ExpiredServiceEventCannotBeDeleted("No puedes cancelar un evento que ya inicio o expiro.");
        }

        serviceEventRepository.delete(serviceEvent);

    }

    @Transactional(readOnly = true)
    @Override
    public List<ServiceEventResponseDto> findAll() {
        return serviceEventRepository.findAll().stream().map(serviceEventMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public ServiceEvent getActiveEventForBranch(String branchId) {
        LocalDateTime now = LocalDateTime.now();

        return serviceEventRepository
                .findFirstByBranchIdAndStartDateTimeLessThanEqualAndEndDateTimeGreaterThanEqual(
                        branchId, now, now
                )
                .orElseThrow(() ->
                        new NoActiveServiceEventException("No hay un servicio activo en este momento."));
    }

    @Transactional(readOnly = true)
    @Override
    public List<ServiceEventResponseDto> getActiveEventForUser(User user) {

        String branchId = String.valueOf(user.getMember()
                .getBranch()
                .getId());

        LocalDateTime now = LocalDateTime.now();

        List<ServiceEvent> event = serviceEventRepository
                .findByBranch_IdAndStartDateTimeLessThanEqualAndEndDateTimeGreaterThanEqual(
                        branchId, now, now);

        return event.stream().map(serviceEventMapper::toDto).toList();
    }


}
