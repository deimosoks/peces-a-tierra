package org.icc.pecesatierra.utils.specs;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.icc.pecesatierra.dtos.member.MemberFilterRequestDto;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class MemberSpecification {

    public Specification<Member> build(MemberFilterRequestDto dto, User user) {
        return (Root<Member> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (dto == null) {
                return cb.conjunction();
            }

            if (dto.getMemberType() != null && !dto.getMemberType().isEmpty()) {
                predicates.add(root.get("typeId").get("id").in(dto.getMemberType()));
            }
            if (dto.getMemberCategory() != null && !dto.getMemberCategory().isEmpty()) {
                predicates.add(root.get("categoryId").get("id").in(dto.getMemberCategory()));
            }
            if (dto.getSubCategory() != null && !dto.getSubCategory().isEmpty()) {
                predicates.add(root.get("subcategoryId").get("id").in(dto.getSubCategory()));
            }

            if (!user.hasAuthority("ADMINISTRATOR")) {
                predicates.add(cb.equal(root.get("branch").get("id"), user.getMember().getBranch().getId()));
            } else if (dto.getBranchId() != null) {
                predicates.add(root.get("branch").get("id").in(dto.getBranchId()));
            }

            if (dto.getGender() != null && !dto.getGender().isEmpty()) {
                predicates.add(cb.equal(root.get("gender"), dto.getGender()));
            }

            if (Boolean.TRUE.equals(dto.getOnlyActive())) {
                predicates.add(cb.equal(root.get("active"), true));
            }

            if (Boolean.TRUE.equals(dto.getHasCc())) predicates.add(cb.isNotNull(root.get("cc")));
            if (Boolean.TRUE.equals(dto.getHasCellphone())) predicates.add(cb.isNotNull(root.get("cellphone")));
            if (Boolean.TRUE.equals(dto.getHasAddress())) predicates.add(cb.isNotNull(root.get("address")));
            if (Boolean.TRUE.equals(dto.getHasBirthdate())) predicates.add(cb.isNotNull(root.get("birthdate")));

            if (Boolean.FALSE.equals(dto.getHasCc())) predicates.add(cb.isNull(root.get("cc")));
            if (Boolean.FALSE.equals(dto.getHasCellphone())) predicates.add(cb.isNull(root.get("cellphone")));
            if (Boolean.FALSE.equals(dto.getHasAddress())) predicates.add(cb.isNull(root.get("address")));
            if (Boolean.FALSE.equals(dto.getHasBirthdate())) predicates.add(cb.isNull(root.get("birthdate")));

            if (dto.getQuery() != null && !dto.getQuery().isBlank()) {
                String searchLike = "%" + dto.getQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("completeName")), searchLike),
                        cb.like(root.get("cc"), searchLike),
                        cb.like(root.get("cellphone"), searchLike),
                        cb.like(root.get("id"), searchLike)
                ));
            }

            if (dto.getAgeFilterRange1() != null && dto.getAgeFilterRange2() != null) {
                int minAge = Math.min(dto.getAgeFilterRange1(), dto.getAgeFilterRange2());
                int maxAge = Math.max(dto.getAgeFilterRange1(), dto.getAgeFilterRange2());
                LocalDate today = LocalDate.now();
                LocalDate maxBirthdate = today.minusYears(minAge);
                LocalDate minBirthdate = today.minusYears(maxAge);
                predicates.add(cb.between(root.get("birthdate"), minBirthdate, maxBirthdate));
            }

            if (dto.getLocation() != null && !dto.getLocation().isBlank()) {
                String searchLike = "%" + dto.getLocation().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("address")), searchLike),
                        cb.like(cb.lower(root.get("neighborhood")), searchLike),
                        cb.like(cb.lower(root.get("city")), searchLike),
                        cb.like(cb.lower(root.get("municipality")), searchLike),
                        cb.like(cb.lower(root.get("district")), searchLike)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
