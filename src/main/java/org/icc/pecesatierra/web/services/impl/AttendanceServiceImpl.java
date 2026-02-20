package org.icc.pecesatierra.web.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.attendance.*;
import org.icc.pecesatierra.entities.*;
import org.icc.pecesatierra.exceptions.*;
import org.icc.pecesatierra.repositories.*;
import org.icc.pecesatierra.utils.mappers.AttendanceMapper;
import org.icc.pecesatierra.web.services.AttendanceService;
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
    private final MemberRepository memberRepository;
    private final AttendanceMapper attendanceMapper;
    private final ServiceEventRepository serviceEventRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    @Override
    public void create(List<AttendanceRequestDto> attendances, User user) {

        if (attendances == null || attendances.isEmpty())
            return;

        ServiceEvent event = serviceEventRepository.findById(attendances.getFirst().getServiceEventId())
                .orElseThrow(() -> new ServiceEventNotFoundException("Evento no encontrado."));

        if (!event.getServices().isActive()) {
            throw new ServicesNotFoundException("Servicio inactivo");
        }

        for (AttendanceRequestDto dto : attendances) {

            Member member = memberRepository.findById(dto.getMemberId())
                    .orElseThrow(() -> new MemberNotFoundException("Miembro no existe."));

            if (!member.isActive()) {
                throw new MemberDeactivatedException("Miembro inactivo.");
            }

            LocalDateTime arrival = dto.getAttendanceDate();

            if (!user.hasAuthority("ADMINISTRATOR")) {
                if (arrival.isBefore(event.getStartDateTime()) ||
                        arrival.isAfter(event.getEndDateTime())) {

                    throw new AttendanceOutOfRangeException(
                            "No puede registrar asistencias en un evento finalizado.");
                }
            }

            if (attendanceRepository.existsByMemberAndServiceEvent(member, event)) {
                continue;
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

            attendanceRepository.save(attendance);
        }
    }

    @Transactional
    @Override
    public AttendanceResponseDto invalidate(AttendanceInvalidRequestDto attendanceInvalidRequestDto, User user) {
        Attendance attendance = attendanceRepository.findById(attendanceInvalidRequestDto.getAttendanceId())
                .orElseThrow(() -> new AttendanceNotFoundException("Esta asistencia no existe."));

        if (!user.hasAuthority("ADMINISTRATOR")
                && !user.getMember().getBranch().getId().equals(attendance.getBranch().getId())) {
            throw new CannotDeleteMemberOutSideYourBranchException(
                    "No puedes invalidar una asistencia fuera de tu sede.");
        }

        attendance.setInvalid(true);
        attendance.setInvalidAt(LocalDateTime.now());
        attendance.setInvalidatorId(user.getMember());
        attendance.setInvalidReason(attendanceInvalidRequestDto.getInvalidReason());

        return attendanceMapper.toDto(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    @Override
    public AttendancePagesResponseDto findAll(int page, AttendanceFiltersRequestDto dto, User user) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by("attendanceDate").descending());

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Attendance> cq = cb.createQuery(Attendance.class);
        Root<Attendance> attendance = cq.from(Attendance.class);

        Join<Attendance, ServiceEvent> serviceEvent = attendance.join("serviceEvent", JoinType.INNER);
        Join<ServiceEvent, Services> service = serviceEvent.join("services", JoinType.INNER);
        Join<Attendance, Member> member = attendance.join("member", JoinType.INNER);
        Join<Attendance, Branch> branch = attendance.join("branch", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();

        if (dto != null) {
            if (dto.getServiceId() != null) {
                predicates.add(cb.equal(service.get("id"), dto.getServiceId()));
            }
            if (dto.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(serviceEvent.get("startDateTime"), dto.getStartDate()));
            }
            if (dto.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(serviceEvent.get("startDateTime"), dto.getEndDate()));
            }
            if (dto.getMemberId() != null) {
                predicates.add(cb.equal(member.get("id"), dto.getMemberId()));
            }
            if (dto.getBranchId() != null) {
                if (user.hasAuthority("ADMINISTRATOR")) {
                    predicates.add(cb.equal(branch.get("id"), dto.getBranchId()));
                } else {
                    predicates.add(cb.equal(branch.get("id"), user.getMember().getBranch().getId()));
                }
            } else if (!user.hasAuthority("ADMINISTRATOR")) {
                predicates.add(cb.equal(branch.get("id"), user.getMember().getBranch().getId()));
            }

            if (dto.getCategory() != null && !dto.getCategory().isEmpty()) {
                Join<Attendance, MemberCategory> memberCategoryJoin = attendance.join("memberCategory", JoinType.INNER);
                predicates.add(memberCategoryJoin.get("name").in(dto.getCategory()));
            }

            if (dto.getSubCategory() != null && !dto.getSubCategory().isEmpty()) {
                Join<Attendance, MemberSubCategory> memberSubCategoryJoin = attendance.join("memberSubCategory",
                        JoinType.INNER);
                predicates.add(memberSubCategoryJoin.get("name").in(dto.getSubCategory()));
            }
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(attendance.get("attendanceDate")));

        TypedQuery<Attendance> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<Attendance> results = query.getResultList();

        // Conteo total
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Attendance> countRoot = countQuery.from(Attendance.class);
        Join<Attendance, ServiceEvent> countServiceEvent = countRoot.join("serviceEvent", JoinType.INNER);
        Join<ServiceEvent, Services> countService = countServiceEvent.join("services", JoinType.INNER);
        Join<Attendance, Member> countMember = countRoot.join("member", JoinType.INNER);
        Join<Attendance, Branch> countBranch = countRoot.join("branch", JoinType.INNER);

        List<Predicate> countPredicates = new ArrayList<>();
        if (dto != null) {
            if (dto.getServiceId() != null) {
                countPredicates.add(cb.equal(countService.get("id"), dto.getServiceId()));
            }
            if (dto.getStartDate() != null) {
                countPredicates
                        .add(cb.greaterThanOrEqualTo(countServiceEvent.get("startDateTime"), dto.getStartDate()));
            }
            if (dto.getEndDate() != null) {
                countPredicates.add(cb.lessThanOrEqualTo(countServiceEvent.get("startDateTime"), dto.getEndDate()));
            }
            if (dto.getMemberId() != null) {
                countPredicates.add(cb.equal(countMember.get("id"), dto.getMemberId()));
            }
            if (dto.getBranchId() != null) {
                if (user.hasAuthority("ADMINISTRATOR")) {
                    countPredicates.add(cb.equal(countBranch.get("id"), dto.getBranchId()));
                } else {
                    countPredicates.add(cb.equal(countBranch.get("id"), user.getMember().getBranch().getId()));
                }
            } else if (!user.hasAuthority("ADMINISTRATOR")) {
                countPredicates.add(cb.equal(countBranch.get("id"), user.getMember().getBranch().getId()));
            }

            if (dto.getCategory() != null && !dto.getCategory().isEmpty()) {
                Join<Attendance, MemberCategory> countMemberCategoryJoin = countRoot.join("memberCategory",
                        JoinType.INNER);
                countPredicates.add(countMemberCategoryJoin.get("name").in(dto.getCategory()));
            }

            if (dto.getSubCategory() != null && !dto.getSubCategory().isEmpty()) {
                Join<Attendance, MemberSubCategory> countMemberSubCategoryJoin = countRoot.join("memberSubCategory",
                        JoinType.INNER);
                countPredicates.add(countMemberSubCategoryJoin.get("name").in(dto.getSubCategory()));
            }
        }

        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = em.createQuery(countQuery).getSingleResult();

        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());

        return new AttendancePagesResponseDto(
                results.stream().map(attendanceMapper::toDto).toList(),
                totalPages);
    }

}
