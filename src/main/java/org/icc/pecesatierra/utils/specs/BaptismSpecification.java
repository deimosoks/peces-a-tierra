package org.icc.pecesatierra.utils.specs;

import jakarta.persistence.criteria.*;
import org.icc.pecesatierra.dtos.baptism.BaptismFilterRequestDto;
import org.icc.pecesatierra.entities.Baptism;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BaptismSpecification {

    public Specification<Baptism> build(BaptismFilterRequestDto dto, User currentUser) {
        return (Root<Baptism> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<Baptism, Member> baptizedMember = root.join("baptizedMember", JoinType.INNER);

            if (dto != null) {

                if (!currentUser.hasAuthority("ADMINISTRATOR")) {
                    predicates.add(cb.equal(baptizedMember.get("branch"), currentUser.getMember().getBranch()));
                } else if (dto.getBranchId() != null) {
                    predicates.add(cb.equal(baptizedMember.get("branch").get("id"), dto.getBranchId()));
                }

                if (dto.getMemberId() != null) {
                    predicates.add(cb.equal(baptizedMember.get("id"), dto.getMemberId()));
                }

                if (dto.getStartDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("date"), dto.getStartDate()));
                }
                if (dto.getEndDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("date"), dto.getEndDate()));
                }

                if (dto.isActive()) {
                    predicates.add(cb.isFalse(root.get("invalid")));
                }

                if (dto.getQuery() != null && !dto.getQuery().isBlank()) {
                    String like = "%" + dto.getQuery().toLowerCase() + "%";

                    predicates.add(cb.or(
                            cb.like(cb.lower(baptizedMember.get("completeName")), like),
                            cb.like(cb.lower(root.get("neighborhood")), like),
                            cb.like(cb.lower(root.get("city")), like),
                            cb.like(cb.lower(root.get("municipality")), like),
                            cb.like(cb.lower(root.get("district")), like),
                            cb.like(cb.lower(root.get("postalCode")), like)
                    ));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
