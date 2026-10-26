package org.icc.pecesatierra.utils.specs;

import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.member.dtos.MemberFilterRequestDto;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.utils.enums.AppPermission;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MemberSpecification {

    private final DateTimeUtils dateTimeUtils;

    public Specification<Member> build(MemberFilterRequestDto dto, User user) {
        // Un DTO nulo se normaliza a filtros vacios en lugar de devolver
        // cb.conjunction(): el filtro de sede debe aplicarse siempre (M-17).
        MemberFilterRequestDto filters = dto == null ? new MemberFilterRequestDto() : dto;

        return (Root<Member> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filters.getMemberType() != null && !filters.getMemberType().isEmpty()) {
                predicates.add(root.get("typeId").get("id").in(filters.getMemberType()));
            }
            if (filters.getMemberCategory() != null && !filters.getMemberCategory().isEmpty()) {
                predicates.add(root.get("categoryId").get("id").in(filters.getMemberCategory()));
            }
            if (filters.getSubCategory() != null && !filters.getSubCategory().isEmpty()) {
                predicates.add(root.get("subcategoryId").get("id").in(filters.getSubCategory()));
            }

            if (!user.hasAuthority(AppPermission.ADMINISTRATOR.name())) {
                predicates.add(cb.equal(root.get("branch").get("id"), user.getMember().getBranch().getId()));
            } else if (filters.getBranchId() != null) {
                predicates.add(root.get("branch").get("id").in(filters.getBranchId()));
            }

            if (filters.getGender() != null && !filters.getGender().isEmpty()) {
                predicates.add(cb.equal(root.get("gender"), filters.getGender()));
            }

            if (Boolean.TRUE.equals(filters.getOnlyActive())) {
                predicates.add(cb.equal(root.get("active"), true));
            }

            if (Boolean.FALSE.equals(filters.getOnlyActive())) {
                predicates.add(cb.equal(root.get("active"), false));
            }

            if (Boolean.TRUE.equals(filters.getHasCc())) predicates.add(cb.isNotNull(root.get("cc")));
            if (Boolean.TRUE.equals(filters.getHasCellphone())) predicates.add(cb.isNotNull(root.get("cellphone")));
            if (Boolean.TRUE.equals(filters.getHasAddress())) predicates.add(cb.isNotNull(root.get("address")));
            if (Boolean.TRUE.equals(filters.getHasBirthdate())) predicates.add(cb.isNotNull(root.get("birthdate")));

            if (Boolean.FALSE.equals(filters.getHasCc())) predicates.add(cb.isNull(root.get("cc")));
            if (Boolean.FALSE.equals(filters.getHasCellphone())) predicates.add(cb.isNull(root.get("cellphone")));
            if (Boolean.FALSE.equals(filters.getHasAddress())) predicates.add(cb.isNull(root.get("address")));
            if (Boolean.FALSE.equals(filters.getHasBirthdate())) predicates.add(cb.isNull(root.get("birthdate")));

            if (filters.getQuery() != null && !filters.getQuery().isBlank()) {
                String normalizedQuery = Normalizer.normalize(filters.getQuery(), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .toLowerCase();

                String[] keywords = normalizedQuery.split("\\s+");
                List<Predicate> namePredicates = new ArrayList<>();

                for (String keyword : keywords) {
                    Expression<String> normalizedName = cb.function(
                            "unaccent", String.class, cb.lower(root.get("completeName"))
                    );

                    String searchLike = "%" + keyword + "%";
                    namePredicates.add(cb.like(normalizedName, searchLike));
                }

                Predicate completeNamePredicate = cb.and(namePredicates.toArray(new Predicate[0]));

                String searchLikeFull = "%" + normalizedQuery + "%";
                Predicate otherFieldsPredicate = cb.or(
                        cb.like(root.get("cc"), searchLikeFull),
                        cb.like(root.get("cellphone"), searchLikeFull),
                        cb.like(root.get("id"), searchLikeFull)
                );

                predicates.add(cb.or(completeNamePredicate, otherFieldsPredicate));
            }

            if (filters.getAgeFilterRange1() != null && filters.getAgeFilterRange2() != null) {
                int minAge = Math.min(filters.getAgeFilterRange1(), filters.getAgeFilterRange2());
                int maxAge = Math.max(filters.getAgeFilterRange1(), filters.getAgeFilterRange2());
                LocalDate today = dateTimeUtils.nowColombia().toLocalDate();
                // La edad es un intervalo de anios, no un punto: quien todavia no
                // ha cumplido maxAge+1 tambien tiene maxAge. Por eso el limite
                // inferior de fecha es EXCLUSIVO en el cumpleanios de maxAge+1 y
                // el superior INCLUSIVO en el de minAge (H-43).
                LocalDate maxAgeExclusiveBirthdate = today.minusYears(maxAge + 1L);
                LocalDate minAgeBirthdate = today.minusYears(minAge);
                predicates.add(cb.and(
                        cb.greaterThan(root.get("birthdate"), maxAgeExclusiveBirthdate),
                        cb.lessThanOrEqualTo(root.get("birthdate"), minAgeBirthdate)
                ));
            }

            if (filters.getRegisteredFrom() != null && filters.getRegisteredTo() != null) {
                predicates.add(cb.between(root.get("createdAt"), dateTimeUtils.toUTC(filters.getRegisteredFrom()), dateTimeUtils.toUTC(filters.getRegisteredTo())));
            }

            if (filters.getLocation() != null && !filters.getLocation().isBlank()) {
                String searchLike = "%" + filters.getLocation().toLowerCase() + "%";
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
