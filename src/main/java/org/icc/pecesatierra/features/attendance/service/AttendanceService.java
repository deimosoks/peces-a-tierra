package org.icc.pecesatierra.features.attendance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.features.attendance.Attendance;
import org.icc.pecesatierra.features.attendance.dtos.AttendanceFiltersRequestDto;
import org.icc.pecesatierra.features.attendance.dtos.AttendanceInvalidRequestDto;
import org.icc.pecesatierra.features.attendance.dtos.AttendanceRequestDto;
import org.icc.pecesatierra.features.attendance.dtos.AttendanceResponseDto;
import org.icc.pecesatierra.features.attendance.repository.AttendanceRepository;
import org.icc.pecesatierra.features.service.ServiceEvent;
import org.icc.pecesatierra.features.service.repository.ServiceEventRepository;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.attendance.exceptions.AttendanceNotFoundException;
import org.icc.pecesatierra.features.attendance.exceptions.AttendanceOutOfRangeException;
import org.icc.pecesatierra.features.attendance.exceptions.CannotRegisterAttendanceWithDeactivatedServiceException;
import org.icc.pecesatierra.features.service.service.events.ServiceEventNotFoundException;
import org.icc.pecesatierra.features.member.exceptions.CannotDeleteMemberOutSideYourBranchException;
import org.icc.pecesatierra.features.attendance.mapper.AttendanceMapper;
import org.icc.pecesatierra.utils.enums.AppPermission;
import org.icc.pecesatierra.utils.models.ExportResponseDto;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.icc.pecesatierra.utils.specs.AttendanceSpecification;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService{

    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;
    private final AttendanceMapper attendanceMapper;
    private final ServiceEventRepository serviceEventRepository;
    private final AttendanceSpecification attendanceSpecification;
    private final DateTimeUtils dateTimeUtils;

    @Transactional
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
                log.warn("Usuario {} intento registrar un integrante {} inactivo, no se registró asistencia en el evento {}", user.getMember().getId(), member.getId(), event.getId());
                continue;
            }

            if (existingMemberIds.contains(member.getId()))
                continue;


            if (!user.hasAuthority(AppPermission.ADMINISTRATOR.name())) {
                if (event.getEndDateTime().isBefore(dateTimeUtils.nowUTC()) ||
                        event.getStartDateTime().isAfter(dateTimeUtils.nowUTC())) {

                    throw new AttendanceOutOfRangeException("No puede registrar asistencias en un evento finalizado.");
                }
            }

            if (event.getStartDateTime().isAfter(dateTimeUtils.nowUTC()))
                throw new AttendanceOutOfRangeException("No puedes registrar asistencias en un evento que no ha iniciado.");

            Attendance attendance = Attendance.builder()
                    .member(member)
                    .serviceEvent(event)
                    .branch(event.getBranch())
                    .attendanceDate(dateTimeUtils.toUTC(dto.getAttendanceDate()))
                    .memberCategory(member.getCategoryId())
                    .memberType(member.getTypeId())
                    .memberSubCategory(member.getSubcategoryId())
                    .note(dto.getNote())
                    .invalid(false)
                    .registeredById(user.getMember())
                    .build();

            attendancesList.add(attendance);
            existingMemberIds.add(member.getId());
            log.info("""
                            Usuario: 
                            Id: {}
                            Nombre: {}
                            Registró la asistencia de:
                            Id: {}
                            Nombre: {} 
                            En el evento: 
                            Id: {}
                            Nombre: {}
                            Hora: {} - {}
                            """
                    , user.getMember().getId()
                    , user.getMember().getCompleteName()
                    , member.getId()
                    , member.getCompleteName()
                    , event.getId()
                    , event.getServices().getName()
                    , event.getStartDateTime()
                    , event.getEndDateTime()
            );

        }

        attendanceRepository.saveAll(attendancesList);
    }

    @Transactional
    public AttendanceResponseDto invalidate(AttendanceInvalidRequestDto attendanceInvalidRequestDto, User user) {
        Attendance attendance = attendanceRepository.findById(attendanceInvalidRequestDto.getAttendanceId())
                .orElseThrow(AttendanceNotFoundException::new);

        if (!user.hasAuthority(AppPermission.ADMINISTRATOR.name())
                && !user.getMember().getBranch().getId().equals(attendance.getBranch().getId())) {
            throw new CannotDeleteMemberOutSideYourBranchException();
        }

        attendance.setInvalid(true);
        attendance.setInvalidAt(dateTimeUtils.nowUTC());
        attendance.setInvalidatorId(user.getMember());
        attendance.setInvalidReason(attendanceInvalidRequestDto.getInvalidReason());

        log.info("Usuario {} invalido la asistencia {}", user.getMember().getId(), attendance.getId());

        return attendanceMapper.toDto(attendance);
    }

    @Transactional(readOnly = true)
    public PagesResponseDto<AttendanceResponseDto> search(int page, AttendanceFiltersRequestDto dto, User user) {

        Specification<Attendance> spec = attendanceSpecification.build(dto, user);

        Sort sort = Sort.by("serviceEvent.startDateTime").ascending();

        if (dto.getOrderBy() != null) {
            sort = dto.getOrderBy().isAsc()
                    ? Sort.by(dto.getOrderBy().getOrderBy()).ascending()
                    : Sort.by(dto.getOrderBy().getOrderBy()).descending();
        }

        Page<Attendance> pageResult = attendanceRepository.findAll(
                spec,
                PageRequest.of(
                        page,
                        20,
                        sort
                )
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
