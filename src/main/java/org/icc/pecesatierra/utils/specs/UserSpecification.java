package org.icc.pecesatierra.utils.specs;

import jakarta.persistence.criteria.*;
import org.icc.pecesatierra.dtos.user.UserFilterRequestDto;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserSpecification {

    public Specification<User> build(UserFilterRequestDto dto, User currentUser) {
        return (Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<User, Member> memberJoin = root.join("member", JoinType.INNER);

            if (dto.getQuery() != null && !dto.getQuery().isBlank()) {
                String searchLike = "%" + dto.getQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), searchLike),
                        cb.like(cb.lower(memberJoin.get("completeName")), searchLike)
                ));
            }

            if (!currentUser.hasAuthority("ADMINISTRATOR")) {
                predicates.add(cb.equal(memberJoin.get("branch").get("id"),
                        currentUser.getMember().getBranch().getId()));
            } else if (dto.getBranchId() != null && !dto.getBranchId().isBlank()) {
                predicates.add(cb.equal(memberJoin.get("branch").get("id"), dto.getBranchId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
