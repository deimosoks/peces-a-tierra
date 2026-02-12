package org.icc.pecesatierra.web.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.attendance.*;
import org.icc.pecesatierra.entities.*;
import org.icc.pecesatierra.exceptions.AttendanceNotFoundException;
import org.icc.pecesatierra.exceptions.MemberNotFoundException;
import org.icc.pecesatierra.exceptions.ServicesNotFoundException;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.ServiceRepository;
import org.icc.pecesatierra.utils.mappers.AttendanceMapper;
import org.icc.pecesatierra.web.services.AttendanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ServiceRepository serviceRepository;
    private final MemberRepository memberRepository;
    private final AttendanceMapper attendanceMapper;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    @Override
    public void create(List<AttendanceRequestDto> attendances, User user) {

        attendances.forEach(attendanceRequestDto -> {

            Services service = serviceRepository.findById(attendanceRequestDto.getServiceId())
                    .orElseThrow(() -> new ServicesNotFoundException("Este servicio no existe."));

            if (!service.isActive())
                throw new ServicesNotFoundException("No puede registrar una asistencia con un servicio inactivo.");

            Member member = memberRepository.findById(attendanceRequestDto.getMemberId())
                    .orElseThrow(() -> new MemberNotFoundException("Este integrante no existe."));

            if (!member.isActive())
                throw new ServicesNotFoundException("No puede registrar una asistencia con un miembro inactivo.");

            AttendanceId attendanceId = AttendanceId.builder()
                    .serviceId(service.getId())
                    .memberId(member.getId())
                    .serviceDate(attendanceRequestDto.getServiceDate())
                    .build();

            Attendance attendance = Attendance.builder()
                    .id(attendanceId)
                    .member(member)
                    .services(service)
                    .memberCategory(member.getCategoryId())
                    .memberType(member.getTypeId())
                    .attendanceDate(attendanceRequestDto.getAttendanceDate())
                    .invalid(false)
                    .note(attendanceRequestDto.getNote())
                    .registeredById(user.getMember())
                    .build();

            attendanceRepository.save(attendance);
        });
    }

    @Transactional
    @Override
    public AttendanceResponseDto invalidate(AttendanceInvalidRequestDto attendanceInvalidRequestDto, User user) {
        AttendanceId attendanceIdRequest = AttendanceId.builder()
                .serviceId(attendanceInvalidRequestDto.getAttendanceId().getServiceId())
                .memberId(attendanceInvalidRequestDto.getAttendanceId().getMemberId())
                .serviceDate(attendanceInvalidRequestDto.getAttendanceId().getServiceDate())
                .build();
        Attendance attendance = attendanceRepository.findById(attendanceIdRequest)
                .orElseThrow(() -> new AttendanceNotFoundException("Esta asistencia no existe."));

        attendance.setInvalid(true);
        attendance.setInvalidAt(LocalDateTime.now());
        attendance.setInvalidatorId(user.getMember());
        attendance.setInvalidReason(attendanceInvalidRequestDto.getInvalidReason());

        return attendanceMapper.toDto(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    @Override
    public AttendancePagesResponseDto findAll(int page, AttendanceFiltersRequestDto dto) {

        Pageable pageable = PageRequest.of(page, 20, Sort.by("id.serviceDate").descending());

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Attendance> cq = cb.createQuery(Attendance.class);
        Root<Attendance> attendance = cq.from(Attendance.class);

        Fetch<Attendance, Services> serviceFetch = attendance.fetch("services", JoinType.INNER);
        Fetch<Attendance, Member> memberFetch = attendance.fetch("member", JoinType.INNER);

        Join<Attendance, Services> service = (Join<Attendance, Services>) serviceFetch;
        Join<Attendance, Member> member = (Join<Attendance, Member>) memberFetch;

        List<Predicate> predicates = new ArrayList<>();

        if (dto != null) {
            if (dto.getServiceId() != null) {
                predicates.add(cb.equal(service.get("id"), dto.getServiceId()));
            }
            if (dto.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(attendance.get("id").get("serviceDate"), dto.getStartDate()));
            }
            if (dto.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(attendance.get("id").get("serviceDate"), dto.getEndDate()));
            }
            if (dto.getMemberId() != null) {
                predicates.add(cb.equal(member.get("id"), dto.getMemberId()));
            }
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(attendance.get("id").get("serviceDate")));

        TypedQuery<Attendance> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Attendance> results = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Attendance> countRoot = countQuery.from(Attendance.class);
        Join<Attendance, Services> countService = countRoot.join("services", JoinType.LEFT);

        List<Predicate> countPredicates = new ArrayList<>();
        if (dto != null) {
            if (dto.getServiceId() != null) {
                countPredicates.add(cb.equal(countService.get("id"), dto.getServiceId()));
            }
            if (dto.getStartDate() != null) {
                countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("id").get("serviceDate"), dto.getStartDate()));
            }
            if (dto.getEndDate() != null) {
                countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("id").get("serviceDate"), dto.getEndDate()));
            }
        }
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = em.createQuery(countQuery).getSingleResult();

        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());

        return new AttendancePagesResponseDto(
                results.stream().map(attendanceMapper::toDto).toList(),
                totalPages
        );
    }

}
