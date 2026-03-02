package org.icc.pecesatierra.utils.specs;

import jakarta.persistence.criteria.*;
import org.icc.pecesatierra.dtos.attendance.AttendanceFiltersRequestDto;
import org.icc.pecesatierra.entities.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AttendanceSpecification {

    public Specification<Attendance> build(AttendanceFiltersRequestDto dto, User user) {
        return (Root<Attendance> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<Attendance, ServiceEvent> serviceEvent = root.join("serviceEvent", JoinType.INNER);
            Join<ServiceEvent, Services> service = serviceEvent.join("services", JoinType.INNER);
            Join<Attendance, Member> member = root.join("member", JoinType.INNER);
            Join<Attendance, Branch> branch = root.join("branch", JoinType.INNER);

            if (dto != null) {
                if (dto.getServiceId() != null) {
                    predicates.add(cb.equal(service.get("id"), dto.getServiceId()));
                }
                if (dto.getEventId() != null) {
                    predicates.add(cb.equal(serviceEvent.get("id"), dto.getEventId()));
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

                if (dto.getInvalid() != null) {
                    predicates.add(cb.equal(root.get("invalid"), dto.getInvalid()));
                }

                if (!user.hasAuthority("ADMINISTRATOR")) {
                    predicates.add(cb.equal(branch.get("id"), user.getMember().getBranch().getId()));
                } else if (dto.getBranchId() != null) {
                    predicates.add(cb.equal(branch.get("id"), dto.getBranchId()));
                }

                if (dto.getCategory() != null && !dto.getCategory().isEmpty()) {
                    Join<Attendance, MemberCategory> memberCategoryJoin = root.join("memberCategory", JoinType.INNER);
                    predicates.add(memberCategoryJoin.get("id").in(dto.getCategory()));
                }

                if (dto.getSubCategory() != null && !dto.getSubCategory().isEmpty()) {
                    Join<Attendance, MemberSubCategory> memberSubCategoryJoin = root.join("memberSubCategory", JoinType.INNER);
                    predicates.add(memberSubCategoryJoin.get("id").in(dto.getSubCategory()));
                }

                if (dto.getGender() != null && !dto.getGender().isEmpty()) {
                    predicates.add(cb.equal(member.get("gender"), dto.getGender()));
                }

            }

            assert query != null;
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(cb.desc(root.get("attendanceDate")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
