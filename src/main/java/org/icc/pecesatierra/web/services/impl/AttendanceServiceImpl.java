package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.attendance.*;
import org.icc.pecesatierra.entities.*;
import org.icc.pecesatierra.exceptions.attendances.AttendanceNotFoundException;
import org.icc.pecesatierra.exceptions.attendances.AttendanceOutOfRangeException;
import org.icc.pecesatierra.exceptions.attendances.CannotRegisterAttendanceWithDeactivatedServiceException;
import org.icc.pecesatierra.exceptions.events.ServiceEventNotFoundException;
import org.icc.pecesatierra.exceptions.members.CannotDeleteMemberOutSideYourBranchException;
import org.icc.pecesatierra.repositories.*;
import org.icc.pecesatierra.utils.mappers.AttendanceMapper;
import org.icc.pecesatierra.utils.models.ExportResponseDto;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.icc.pecesatierra.utils.specs.AttendanceSpecification;
import org.icc.pecesatierra.web.services.AttendanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;
    private final AttendanceMapper attendanceMapper;
    private final ServiceEventRepository serviceEventRepository;
    private final AttendanceSpecification attendanceSpecification;

    @Transactional
    @Override
    public void create(List<AttendanceRequestDto> attendances, User user) {

        if (attendances == null || attendances.isEmpty())
            return;

        ServiceEvent event = serviceEventRepository.findById(attendances.getFirst().getServiceEventId())
                .orElseThrow(ServiceEventNotFoundException::new);

        if (!event.getServices().isActive()) {
            throw new CannotRegisterAttendanceWithDeactivatedServiceException(event.getServices().getName());
        }

        Set<String> memberIds = attendances.stream().map(AttendanceRequestDto::getMemberId).collect(Collectors.toSet());

        Map<String, Member> members = memberRepository.findAllById(memberIds)
                .stream().collect(Collectors.toMap(Member::getId, m -> m));

        Set<String> existingMemberIds =
                attendanceRepository.findMemberIdsByServiceEventIdInvalidFalse(event.getId());

        Set<Attendance> attendancesList = new HashSet<>();

        for (AttendanceRequestDto dto : attendances) {

            Member member = members.get(dto.getMemberId());

            if (member == null) {
                log.warn("Usuario {} intento registrar una asistencia con un integrante que no existe.", user.getMember().getId());
                continue;
            }

            if (!member.isActive()) {
                log.warn("Usuario {} intento registrar un integrante {} inactivo, no se registró asistencia en el evento {}",user.getMember().getId(), member.getId(), event.getId());
                continue;
            }

            if (existingMemberIds.contains(member.getId()))
                continue;

            LocalDateTime arrival = dto.getAttendanceDate();

            if (!user.hasAuthority("ADMINISTRATOR")) {
                if (arrival.isBefore(event.getStartDateTime()) ||
                        arrival.isAfter(event.getEndDateTime())) {

                    throw new AttendanceOutOfRangeException();
                }
            }

            Attendance attendance = Attendance.builder()
                    .member(member)
                    .serviceEvent(event)
                    .branch(event.getBranch())
                    .attendanceDate(arrival)
                    .memberCategory(member.getCategoryId())
                    .memberType(member.getTypeId())
                    .memberSubCategory(member.getSubcategoryId())
                    .note(dto.getNote())
                    .invalid(false)
                    .registeredById(user.getMember())
                    .build();

            attendancesList.add(attendance);
            existingMemberIds.add(member.getId());
            log.info("Usuario {} registró la asistencia de {} en el evento {}", user.getMember().getId(), member.getId(), event.getId());

        }

        attendanceRepository.saveAll(attendancesList);
    }

    @Transactional
    @Override
    public AttendanceResponseDto invalidate(AttendanceInvalidRequestDto attendanceInvalidRequestDto, User user) {
        Attendance attendance = attendanceRepository.findById(attendanceInvalidRequestDto.getAttendanceId())
                .orElseThrow(AttendanceNotFoundException::new);

        if (!user.hasAuthority("ADMINISTRATOR")
                && !user.getMember().getBranch().getId().equals(attendance.getBranch().getId())) {
            throw new CannotDeleteMemberOutSideYourBranchException();
        }

        attendance.setInvalid(true);
        attendance.setInvalidAt(LocalDateTime.now());
        attendance.setInvalidatorId(user.getMember());
        attendance.setInvalidReason(attendanceInvalidRequestDto.getInvalidReason());

        log.info("Usuario {} invalido la asistencia {}", user.getMember().getId(), attendance.getId());

        return attendanceMapper.toDto(attendance);
    }

    @Transactional(readOnly = true)
    @Override
    public PagesResponseDto<AttendanceResponseDto> search(int page, AttendanceFiltersRequestDto dto, User user) {

        Specification<Attendance> spec = attendanceSpecification.build(dto, user);

        Page<Attendance> pageResult = attendanceRepository.findAll(
                spec,
                PageRequest.of(page, 20, Sort.by("serviceEvent.startDateTime"))
        );

        return PagesResponseDto.<AttendanceResponseDto>builder()
                .data(pageResult.stream().map(attendanceMapper::toDto).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalPages(pageResult.getTotalPages())
                .totalElements(pageResult.getTotalElements())
                .build();

    }

    @Transactional(readOnly = true)
    @Override
    public ExportResponseDto<AttendanceResponseDto> export(AttendanceFiltersRequestDto dto, User user) {
        Specification<Attendance> spec = attendanceSpecification.build(dto, user);

        List<Attendance> members = attendanceRepository.findAll(spec);

        log.info("Usuario {} exporto una lista de asistencias.", user.getMember().getId());

        return ExportResponseDto.<AttendanceResponseDto>builder()
                .data(members.stream().map(attendanceMapper::toDto).toList())
                .totalElements(members.size())
                .build();
    }

}
