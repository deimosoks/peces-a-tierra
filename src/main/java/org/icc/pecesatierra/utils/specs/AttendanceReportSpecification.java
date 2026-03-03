package org.icc.pecesatierra.utils.specs;

import jakarta.persistence.criteria.*;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.entities.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class AttendanceReportSpecification {

    public Specification<Attendance> build(ReportRequestDto dto) {

        return (Root<Attendance> root,
                CriteriaQuery<?> query,
                CriteriaBuilder cb) -> {

            Join<Attendance, ServiceEvent> serviceEvent =
                    root.join("serviceEvent", JoinType.INNER);

            Join<ServiceEvent, Services> service =
                    serviceEvent.join("services", JoinType.INNER);

            Join<Attendance, Branch> branch =
                    root.join("branch", JoinType.INNER);

            Join<Attendance, MemberCategory> category =
                    root.join("memberCategory", JoinType.INNER);

            Join<Attendance, MemberType> type =
                    root.join("memberType", JoinType.INNER);

            Join<Attendance, MemberSubCategory> subCategory =
                    root.join("memberSubCategory", JoinType.LEFT);

            Expression<LocalDateTime> serviceDateTime =
                    serviceEvent.get("startDateTime");

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("invalid")));

            if (dto.getStartDate() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(serviceDateTime, dto.getStartDate())
                );
            }

            if (dto.getEndDate() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(serviceDateTime, dto.getEndDate())
                );
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
                predicates.add(
                        cb.equal(root.get("member").get("id"), dto.getUserId())
                );
            }

            if (dto.getEventId() != null && !dto.getEventId().isEmpty()) {
                predicates.add(
                        cb.equal(serviceEvent.get("id"), dto.getEventId())
                );
            }

            if (dto.isOnlyActive()) {
                predicates.add(
                        cb.isTrue(root.get("member").get("active"))
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
