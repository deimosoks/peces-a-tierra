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
import org.icc.pecesatierra.repositories.UserRepository;
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

    // private MemberRepository memberRepository;
    // private ServiceRepository serviceRepository;
    // private AttendanceRepository attendanceRepository;
    //
    // private final MemberTypeMapper memberTypeMapper;
    // private final MemberCategoryMapper memberCategoryMapper;

    @PersistenceContext
    private EntityManager em;

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public List<ReportResponseDto> generate(ReportRequestDto dto) {
        // Enforce Scoping for Non-Admins
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            org.icc.pecesatierra.entities.User user = userRepository.findByUsername(username).orElse(null);

            if (user != null) {
                boolean isAdmin = user.hasAuthority("ADMINISTRATOR");

                if (!isAdmin) {
                    org.icc.pecesatierra.entities.Branch branch = user.getMember().getBranch();
                    if (branch != null) {
                        dto.setBranchIds(java.util.List.of(branch.getId()));
                    } else {
                        // User has no branch and no admin role -> Return empty
                        dto.setBranchIds(java.util.List.of("NON_EXISTENT_BRANCH"));
                    }
                }
            }
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ReportResponseDto> cq = cb.createQuery(ReportResponseDto.class);

        Root<Attendance> attendance = cq.from(Attendance.class);

        // Joins
        Join<Attendance, ServiceEvent> serviceEvent = attendance.join("serviceEvent", JoinType.INNER);
        Join<ServiceEvent, Services> service = serviceEvent.join("services", JoinType.INNER);
        Join<Attendance, Branch> branch = attendance.join("branch", JoinType.INNER);

        Join<Attendance, MemberCategory> category = attendance.join("memberCategory", JoinType.INNER);
        Join<Attendance, MemberType> type = attendance.join("memberType", JoinType.INNER);
        Join<Attendance, MemberSubCategory> subCategory = attendance.join("memberSubCategory", JoinType.LEFT);

        // Basic Expressions
        Expression<LocalDateTime> serviceDateTime = serviceEvent.get("startDateTime");
        Expression<LocalDate> onlyDate = cb.function("DATE", LocalDate.class, serviceDateTime);
        Expression<String> subCategoryName = cb.coalesce(subCategory.get("name"), "");

        // Predicates
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

        // SubCategories Filter
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

        if (dto.isOnlyActive()) {
            predicates.add(cb.isTrue(attendance.get("member").get("active")));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        // Dynamic Grouping
        List<String> groupBy = dto.getGroupBy();
        if (groupBy == null || groupBy.isEmpty()) {
            groupBy = List.of("DATE", "SERVICE", "CATEGORY", "TYPE", "SUBCATEGORY", "BRANCH");
        }

        List<Selection<?>> selections = new ArrayList<>();
        List<Expression<?>> groupExpressions = new ArrayList<>();

        // 1. Date
        if (groupBy.contains("DATE")) {
            selections.add(onlyDate);
            selections.add(serviceDateTime);
            groupExpressions.add(onlyDate);
            groupExpressions.add(serviceDateTime);
        } else {
            selections.add(cb.nullLiteral(LocalDate.class));
            selections.add(cb.nullLiteral(LocalDateTime.class));
        }

        // 2. Service Name
        if (groupBy.contains("SERVICE")) {
            selections.add(service.get("name"));
            groupExpressions.add(service.get("name"));
            // Also adding ID to group by correctly if needed, but for now name is enough
            // unique usually
        } else {
            selections.add(cb.nullLiteral(String.class));
        }

        // 3. Category
        if (groupBy.contains("CATEGORY")) {
            selections.add(category.get("name"));
            groupExpressions.add(category.get("name"));
            groupExpressions.add(category.get("id"));
        } else {
            selections.add(cb.nullLiteral(String.class));
        }

        // 4. Type
        if (groupBy.contains("TYPE")) {
            selections.add(type.get("name"));
            groupExpressions.add(type.get("name"));
            groupExpressions.add(type.get("id"));
        } else {
            selections.add(cb.nullLiteral(String.class));
        }

        // 5. SubCategory
        if (groupBy.contains("SUBCATEGORY")) {
            selections.add(subCategoryName);
            groupExpressions.add(subCategoryName);
            groupExpressions.add(subCategory.get("id"));
        } else {
            selections.add(cb.nullLiteral(String.class));
        }

        // 6. Branch (New)
        if (groupBy.contains("BRANCH")) {
            selections.add(branch.get("name"));
            groupExpressions.add(branch.get("name"));
            groupExpressions.add(branch.get("id"));
        } else {
            selections.add(cb.nullLiteral(String.class));
        }

        // Count
        selections.add(cb.count(attendance));

        cq.multiselect(selections);
        cq.groupBy(groupExpressions);

        // Order By
        if (groupBy.contains("DATE")) {
            cq.orderBy(cb.asc(onlyDate), cb.asc(serviceDateTime));
        }

        return em.createQuery(cq).getResultList();
    }

    public static LocalDateTime randomDateTime(
            LocalDateTime start,
            LocalDateTime end) {
        long startSeconds = start.toEpochSecond(ZoneOffset.UTC);
        long endSeconds = end.toEpochSecond(ZoneOffset.UTC);

        long randomSeconds = ThreadLocalRandom
                .current()
                .nextLong(startSeconds, endSeconds);

        return LocalDateTime.ofEpochSecond(
                randomSeconds,
                0,
                ZoneOffset.UTC);
    }

}
