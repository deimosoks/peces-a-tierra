package org.icc.pecesatierra.web.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.service.event.ServiceEventRequestDto;
import org.icc.pecesatierra.dtos.service.event.ServiceEventResponseDto;
import org.icc.pecesatierra.dtos.service.event.ServiceEventsFilterRequestDto;
import org.icc.pecesatierra.entities.Branch;
import org.icc.pecesatierra.entities.ServiceEvent;
import org.icc.pecesatierra.entities.Services;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.events.StartDateCannotBeAfterTheEndDateException;
import org.icc.pecesatierra.exceptions.branches.BranchNotFoundException;
import org.icc.pecesatierra.exceptions.events.CannotCancelEventOutsideYouBranch;
import org.icc.pecesatierra.exceptions.events.CannotCancelEventsWithRecords;
import org.icc.pecesatierra.exceptions.events.ServiceEventNotFoundException;
import org.icc.pecesatierra.exceptions.services.InvalidStartAndEndDatesException;
import org.icc.pecesatierra.exceptions.services.ServicesNotFoundException;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.BranchRepository;
import org.icc.pecesatierra.repositories.ServiceEventRepository;
import org.icc.pecesatierra.repositories.ServiceRepository;
import org.icc.pecesatierra.utils.mappers.ServiceEventMapper;
import org.icc.pecesatierra.utils.specs.ServiceEventSpecification;
import org.icc.pecesatierra.web.services.ServiceEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceEventServiceImpl implements ServiceEventService {

    private final ServiceEventRepository serviceEventRepository;
    private final ServiceEventMapper serviceEventMapper;
    private final ServiceRepository serviceRepository;
    private final BranchRepository branchRepository;
    private final AttendanceRepository attendanceRepository;
    private final ServiceEventSpecification serviceEventSpecification;

    @Transactional
    @Override
    public ServiceEventResponseDto create(ServiceEventRequestDto serviceEventRequestDto, User user) {

        if (serviceEventRequestDto.getStartDateTime().isAfter(serviceEventRequestDto.getEndDateTime())) {
            log.warn("Usuario {} intento crear evento con startDate {} mayor a endDate {}",
                    user.getMember().getId(), serviceEventRequestDto.getStartDateTime(), serviceEventRequestDto.getEndDateTime());
            throw new StartDateCannotBeAfterTheEndDateException();
        }

        Branch branch;

        if (user.hasAuthority("ADMINISTRATOR") && serviceEventRequestDto.getBranchId() != null) {
            branch = branchRepository.findById(serviceEventRequestDto.getBranchId())
                    .orElseThrow(BranchNotFoundException::new);
        } else {
            branch = user.getMember().getBranch();
        }

        Services service = serviceRepository.findById(serviceEventRequestDto.getServiceId())
                .orElseThrow(ServicesNotFoundException::new);

        ServiceEvent serviceEvent = ServiceEvent.builder()
                .createdBy(user.getMember())
                .branch(branch)
                .services(service)
                .startDateTime(serviceEventRequestDto.getStartDateTime())
                .endDateTime(serviceEventRequestDto.getEndDateTime())
                .build();

        serviceEventRepository.save(serviceEvent);

        log.info("""
                        Usuario {} creó un evento de servicio:
                        ID: {}
                        Servicio: {}
                        Branch: {}
                        StartDate: {}
                        EndDate: {}
                        """,
                user.getMember().getId(),
                serviceEvent.getId(),
                service.getName(),
                branch.getName(),
                serviceEvent.getStartDateTime(),
                serviceEvent.getEndDateTime()
        );

        return serviceEventMapper.toDto(serviceEvent);
    }

    @Transactional
    @Override
    public void cancel(String serviceEventId, User user) {
        ServiceEvent serviceEvent = serviceEventRepository.findById(serviceEventId)
                .orElseThrow(ServiceEventNotFoundException::new);

        if (!user.hasAuthority("ADMINISTRATOR") && !user.getMember().getBranch().getId().equals(serviceEvent.getBranch().getId())) {
            log.warn("Usuario {} intento cancelar evento {} fuera de su branch", user.getMember().getId(), serviceEvent.getId());
            throw new CannotCancelEventOutsideYouBranch();
        }

        if (attendanceRepository.existsByServiceEvent(serviceEvent)) {
            log.warn("Usuario {} intento cancelar evento {} que ya tiene registros", user.getMember().getId(), serviceEvent.getId());
            throw new CannotCancelEventsWithRecords();
        }

        log.info("""
                        Usuario {} canceló el evento de servicio:
                        ID: {}
                        Servicio: {}
                        Branch: {}
                        StartDate: {}
                        EndDate: {}
                        """,
                user.getMember().getId(),
                serviceEvent.getId(),
                serviceEvent.getServices().getName(),
                serviceEvent.getBranch().getName(),
                serviceEvent.getStartDateTime(),
                serviceEvent.getEndDateTime()
        );

        serviceEventRepository.delete(serviceEvent);

    }

    @Transactional(readOnly = true)
    @Override
    public List<ServiceEventResponseDto> findForCalendar(
            ServiceEventsFilterRequestDto dto,
            User user
    ) {

        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new InvalidStartAndEndDatesException();
        }

        Specification<ServiceEvent> spec = serviceEventSpecification.build(dto, user);

        List<ServiceEvent> serviceEvents = serviceEventRepository.findAll(spec);

        return serviceEvents.stream()
                .map(serviceEventMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ServiceEventResponseDto> getActiveEventForUser(User user) {

        String branchId = String.valueOf(user.getMember()
                .getBranch()
                .getId());
        //TODO: implementar lógica de formato de hora luego
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Bogota"));

        List<ServiceEvent> event = serviceEventRepository
                .findByBranch_IdAndStartDateTimeLessThanEqualAndEndDateTimeGreaterThanEqual(
                        branchId, now, now);

        return event.stream().map(serviceEventMapper::toDto).toList();
    }

}
