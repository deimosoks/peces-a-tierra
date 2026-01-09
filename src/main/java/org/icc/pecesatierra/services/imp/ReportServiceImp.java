package org.icc.pecesatierra.services.imp;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.report.ReportResponseDto;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.entities.Attendance;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.Services;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.ServiceRepository;
import org.icc.pecesatierra.services.ReportService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


@Service
@AllArgsConstructor
public class ReportServiceImp implements ReportService {

    @PersistenceContext
    private EntityManager em;

    private MemberRepository memberRepository;
    private ServiceRepository serviceRepository;
    private AttendanceRepository attendanceRepository;

    @Override
    public List<ReportResponseDto> generate(ReportRequestDto dto) {

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

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ReportResponseDto> cq = cb.createQuery(ReportResponseDto.class);

        Root<Attendance> attendance = cq.from(Attendance.class);
        Join<Attendance, Member> member = attendance.join("member");
        Join<Attendance, Services> service = attendance.join("services");

        List<Predicate> predicates = new ArrayList<>();

        if (dto.getCategories() != null && !dto.getCategories().isEmpty()) {
            predicates.add(attendance.get("memberCategory").in(dto.getCategories()));
        }

        if (dto.getTypePeoples() != null && !dto.getTypePeoples().isEmpty()) {
            predicates.add(attendance.get("memberType").in(dto.getTypePeoples()));
        }

        if (dto.getServiceIds() != null && !dto.getServiceIds().isEmpty()) {
            predicates.add(attendance.get("id").get("serviceId").in(dto.getServiceIds()));
        }

        if (dto.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    attendance.get("serviceStartDate"),
                    dto.getStartDate()
            ));
        }

        if (dto.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                    attendance.get("serviceStartDate"),
                    dto.getEndDate()
            ));
        }

        if (dto.getUserId() != null) {
            predicates.add(cb.equal(attendance.get("id").get("memberId"), dto.getUserId()));
        }

        if (dto.isOnlyActive()) {
            predicates.add(cb.isTrue(member.get("active")));
        }

        Path<LocalDateTime> attendanceDateTime = attendance.get("serviceStartDate");

        Expression<LocalDate> serviceAttendanceDate = cb.function("DATE", LocalDate.class, attendanceDateTime);

        cq.select(cb.construct(
                ReportResponseDto.class,
                serviceAttendanceDate,
                attendance.get("serviceStartDate"),
                service.get("name"),
                attendance.get("memberCategory"),
                attendance.get("memberType"),
                cb.count(attendance)
        ));

        cq.where(predicates.toArray(new Predicate[0]));

        cq.groupBy(
                serviceAttendanceDate,
                attendance.get("serviceStartDate"),
                service.get("name"),
                attendance.get("memberCategory"),
                attendance.get("memberType")
        );

        cq.orderBy(
                cb.asc(serviceAttendanceDate),
                cb.asc(attendance.get("serviceStartDate"))
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
