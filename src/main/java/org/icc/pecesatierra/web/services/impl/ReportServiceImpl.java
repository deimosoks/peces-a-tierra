package org.icc.pecesatierra.web.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.report.ReportResponseDto;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.entities.*;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.ServiceRepository;
import org.icc.pecesatierra.utils.mappers.MemberCategoryMapper;
import org.icc.pecesatierra.utils.mappers.MemberTypeMapper;
import org.icc.pecesatierra.web.services.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

//    private MemberRepository memberRepository;
//    private ServiceRepository serviceRepository;
//    private AttendanceRepository attendanceRepository;
//
//    private final MemberTypeMapper memberTypeMapper;
//    private final MemberCategoryMapper memberCategoryMapper;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    @Override
    public List<ReportResponseDto> generate(ReportRequestDto dto) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ReportResponseDto> cq = cb.createQuery(ReportResponseDto.class);

        Root<Attendance> attendance = cq.from(Attendance.class);

        Join<Attendance, Services> service = attendance.join("services", JoinType.INNER);
        Join<Attendance, MemberCategory> category = attendance.join("memberCategory", JoinType.INNER);
        Join<Attendance, MemberType> type = attendance.join("memberType", JoinType.INNER);
        Join<Attendance, MemberSubCategory> subCategory =
                attendance.join("memberSubCategory", JoinType.LEFT);

        Path<LocalDateTime> serviceDateTime =
                attendance.get("id").get("serviceDate");

        Expression<LocalDate> onlyDate =
                cb.function("DATE", LocalDate.class, serviceDateTime);

        Expression<String> subCategoryName =
                cb.coalesce(subCategory.get("name"), "");

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.isFalse(attendance.get("invalid")));

        if (dto.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(serviceDateTime, dto.getStartDate()));
        }

        if (dto.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(serviceDateTime, dto.getEndDate()));
        }

        if (dto.getCategories() != null && !dto.getCategories().isEmpty()) {
            predicates.add(category.get("id").in(dto.getCategories()));
        }

        if (dto.getTypePeoples() != null && !dto.getTypePeoples().isEmpty()) {
            predicates.add(type.get("id").in(dto.getTypePeoples()));
        }

        if (dto.getServiceIds() != null && !dto.getServiceIds().isEmpty()) {
            predicates.add(service.get("id").in(dto.getServiceIds()));
        }

        cq.select(cb.construct(
                ReportResponseDto.class,
                onlyDate,                     // LocalDate
                serviceDateTime,              // LocalDateTime
                service.get("name"),          // String
                category.get("name"),         // String
                type.get("name"),             // String
                subCategoryName,              // String (nullable safe)
                cb.count(attendance)          // Long
        ));

        cq.where(predicates.toArray(new Predicate[0]));

        cq.groupBy(
                onlyDate,
                serviceDateTime,
                service.get("name"),
                category.get("id"),
                category.get("name"),
                type.get("id"),
                type.get("name"),
                subCategory.get("id"),
                subCategory.get("name")
        );

        cq.orderBy(
                cb.asc(onlyDate),
                cb.asc(serviceDateTime)
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
