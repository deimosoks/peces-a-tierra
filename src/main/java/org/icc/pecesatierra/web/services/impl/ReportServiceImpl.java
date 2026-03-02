package org.icc.pecesatierra.web.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.report.ReportResponseDto;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.entities.*;
import org.icc.pecesatierra.repositories.UserRepository;
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
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    @PersistenceContext
    private final EntityManager em;

    @Transactional(readOnly = true)
    @Override
    public List<ReportResponseDto> generate(ReportRequestDto dto, User user) {

        boolean isAdmin = user.hasAuthority("ADMINISTRATOR");

        if (!isAdmin) {
            dto.setBranchIds(java.util.List.of(user.getMember().getBranch().getId()));
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ReportResponseDto> cq = cb.createQuery(ReportResponseDto.class);

        Root<Attendance> attendance = cq.from(Attendance.class);

        Join<Attendance, ServiceEvent> serviceEvent = attendance.join("serviceEvent", JoinType.INNER);
        Join<ServiceEvent, Services> service = serviceEvent.join("services", JoinType.INNER);
        Join<Attendance, Branch> branch = attendance.join("branch", JoinType.INNER);

        Join<Attendance, MemberCategory> category = attendance.join("memberCategory", JoinType.INNER);
        Join<Attendance, MemberType> type = attendance.join("memberType", JoinType.INNER);
        Join<Attendance, MemberSubCategory> subCategory = attendance.join("memberSubCategory", JoinType.LEFT);

        Expression<LocalDateTime> serviceDateTime = serviceEvent.get("startDateTime");
        Expression<LocalDate> onlyDate = cb.function("DATE", LocalDate.class, serviceDateTime);
        Expression<String> subCategoryName = cb.coalesce(subCategory.get("name"), "");

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

        if (dto.getSubCategories() != null && !dto.getSubCategories().isEmpty()) {
            predicates.add(subCategory.get("id").in(dto.getSubCategories()));
        }

        if (dto.getTypePeoples() != null && !dto.getTypePeoples().isEmpty()) {
            predicates.add(type.get("id").in(dto.getTypePeoples()));
        }

        if (dto.getServiceIds() != null && !dto.getServiceIds().isEmpty()) {
            predicates.add(service.get("id").in(dto.getServiceIds()));
        }

        if (dto.getBranchIds() != null && !dto.getBranchIds().isEmpty()) {
            predicates.add(branch.get("id").in(dto.getBranchIds()));
        }

        if (dto.getUserId() != null && !dto.getUserId().isEmpty()) {
            predicates.add(cb.equal(attendance.get("member").get("id"), dto.getUserId()));
        }

        if (dto.getEventId() != null && !dto.getEventId().isEmpty()) {
            predicates.add(cb.equal(serviceEvent.get("id"), dto.getEventId()));
        }

        if (dto.isOnlyActive()) {
            predicates.add(cb.isTrue(attendance.get("member").get("active")));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        List<String> groupBy = dto.getGroupBy();
        if (groupBy == null || groupBy.isEmpty()) {
            groupBy = List.of("DATE", "SERVICE", "CATEGORY", "TYPE", "SUBCATEGORY", "BRANCH");
        }

        List<Selection<?>> selections = new ArrayList<>();
        List<Expression<?>> groupExpressions = new ArrayList<>();

        if (groupBy.contains("DATE")) {
            selections.add(onlyDate);
            selections.add(serviceDateTime);
            groupExpressions.add(onlyDate);
            groupExpressions.add(serviceDateTime);
        } else {
            selections.add(cb.nullLiteral(LocalDate.class));
            selections.add(cb.nullLiteral(LocalDateTime.class));
        }

        if (groupBy.contains("SERVICE")) {
            selections.add(service.get("name"));
            groupExpressions.add(service.get("name"));
        } else {
            selections.add(cb.nullLiteral(String.class));
        }

        if (groupBy.contains("CATEGORY")) {
            selections.add(category.get("name"));
            groupExpressions.add(category.get("name"));
            groupExpressions.add(category.get("id"));
        } else {
            selections.add(cb.nullLiteral(String.class));
        }

        if (groupBy.contains("TYPE")) {
            selections.add(type.get("name"));
            groupExpressions.add(type.get("name"));
            groupExpressions.add(type.get("id"));
        } else {
            selections.add(cb.nullLiteral(String.class));
        }

        if (groupBy.contains("SUBCATEGORY")) {
            selections.add(subCategoryName);
            groupExpressions.add(subCategoryName);
            groupExpressions.add(subCategory.get("id"));
        } else {
            selections.add(cb.nullLiteral(String.class));
        }

        if (groupBy.contains("BRANCH")) {
            selections.add(branch.get("name"));
            groupExpressions.add(branch.get("name"));
            groupExpressions.add(branch.get("id"));
        } else {
            selections.add(cb.nullLiteral(String.class));
        }

        selections.add(cb.count(attendance));

        cq.multiselect(selections);
        cq.groupBy(groupExpressions);

        if (groupBy.contains("DATE")) {
            cq.orderBy(cb.asc(onlyDate), cb.asc(serviceDateTime));
        }

        return em.createQuery(cq).getResultList();
    }

}
