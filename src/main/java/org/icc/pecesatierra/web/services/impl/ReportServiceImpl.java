package org.icc.pecesatierra.web.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.report.ReportResponseDto;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.entities.Attendance;
import org.icc.pecesatierra.entities.AttendanceId;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.Services;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.ServiceRepository;
import org.icc.pecesatierra.web.services.ReportService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;


@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService {

    @PersistenceContext
    private EntityManager em;

    private MemberRepository memberRepository;
    private ServiceRepository serviceRepository;
    private AttendanceRepository attendanceRepository;

    @Override
    public List<ReportResponseDto> generate(ReportRequestDto dto) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ReportResponseDto> cq = cb.createQuery(ReportResponseDto.class);

//        ExecutorService executor = Executors.newFixedThreadPool(30);
//
//        for (int i = 0; i < 60; i++) {
//            executor.submit(DataLoaderService.builder()
//                    .serviceRepository(serviceRepository)
//                    .memberRepository(memberRepository)
//                    .attendanceRepository(attendanceRepository)
//                    .build());
//        }
//
//        executor.shutdown();

        Root<Attendance> attendance = cq.from(Attendance.class);
        Join<Attendance, Member> member = attendance.join("member", JoinType.INNER);
        Join<Attendance, Services> service = attendance.join("services", JoinType.INNER);

        Path<AttendanceId> id = attendance.get("id");
        Path<LocalDateTime> serviceDate = id.get("serviceDate");

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.isFalse(attendance.get("invalid")));

        if (dto.getCategories() != null && !dto.getCategories().isEmpty()) {
            predicates.add(attendance.get("memberCategory").in(dto.getCategories()));
        }

        if (dto.getTypePeoples() != null && !dto.getTypePeoples().isEmpty()) {
            predicates.add(attendance.get("memberType").in(dto.getTypePeoples()));
        }

        if (dto.getServiceIds() != null && !dto.getServiceIds().isEmpty()) {
            predicates.add(id.get("serviceId").in(dto.getServiceIds()));
        }

        if (dto.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(serviceDate, dto.getStartDate()));
        }

        if (dto.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(serviceDate, dto.getEndDate()));
        }

        if (dto.getUserId() != null) {
            predicates.add(cb.equal(id.get("memberId"), dto.getUserId()));
        }

        if (dto.isOnlyActive()) {
            predicates.add(cb.isTrue(member.get("active")));
        }

        Expression<LocalDate> serviceAttendanceDate =
                cb.function("DATE", LocalDate.class, serviceDate);

        cq.select(cb.construct(
                ReportResponseDto.class,
                serviceAttendanceDate,
                serviceDate,
                service.get("name"),
                attendance.get("memberCategory"),
                attendance.get("memberType"),
                cb.count(attendance)
        ));

        cq.where(predicates.toArray(new Predicate[0]));

        cq.groupBy(
                serviceAttendanceDate,
                serviceDate,
                service.get("name"),
                attendance.get("memberCategory"),
                attendance.get("memberType")
        );

        cq.orderBy(
                cb.asc(serviceAttendanceDate),
                cb.asc(serviceDate)
        );

        return em.createQuery(cq).getResultList();
    }


    public static LocalDateTime randomDateTime(
            LocalDateTime start,
            LocalDateTime end
    ) {
        long startSeconds = start.toEpochSecond(ZoneOffset.UTC);
        long endSeconds = end.toEpochSecond(ZoneOffset.UTC);

        long randomSeconds = ThreadLocalRandom
                .current()
                .nextLong(startSeconds, endSeconds);

        return LocalDateTime.ofEpochSecond(
                randomSeconds,
                0,
                ZoneOffset.UTC
        );
    }

}
