package org.icc.pecesatierra.utils.specs;

import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.discipulado.DiscipuladoFilterRequestDto;
import org.icc.pecesatierra.entities.Discipulado;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DiscipuladoSpecification {

    public Specification<Discipulado> build(DiscipuladoFilterRequestDto dto, User user) {
        return (Root<Discipulado> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (dto == null) {
                return cb.conjunction();
            }

            if (user.hasAuthority("ADMINISTRATOR") && dto.getBranchId() != null) {
                predicates.add(cb.equal(root.get("member").get("branch").get("id"), dto.getBranchId()));
            } else {
                predicates.add(cb.equal(root.get("member").get("branch").get("id"), user.getMember().getBranch().getId()));
            }

            // 🔹 Filtrar por miembro (a quién pertenece el discipulado)
            if (dto.getMemberId() != null && !dto.getMemberId().isBlank()) {
                predicates.add(cb.equal(root.get("member").get("id"), dto.getMemberId()));
            }

            // 🔹 Filtrar por teacher (registeredBy)
            if (dto.getTeacherId() != null && !dto.getTeacherId().isBlank()) {
                predicates.add(cb.equal(root.get("registeredBy").get("id"), dto.getTeacherId()));
            }

            // 🔹 Filtrar por estado completado
            if (dto.getCompleted() != null) {
                predicates.add(cb.equal(root.get("completed"), dto.getCompleted()));
            }

            if (dto.getDateStartedFrom() != null && dto.getDateStartedTo() != null) {
                predicates.add(cb.between(
                        root.get("dateStarted"),
                        dto.getDateStartedFrom().atOffset(java.time.ZoneOffset.UTC),
                        dto.getDateStartedTo().atOffset(java.time.ZoneOffset.UTC)
                ));
            } else if (dto.getDateStartedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("dateStarted"),
                        dto.getDateStartedFrom().atOffset(java.time.ZoneOffset.UTC)
                ));
            } else if (dto.getDateStartedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("dateStarted"),
                        dto.getDateStartedTo().atOffset(java.time.ZoneOffset.UTC)
                ));
            }

            if (dto.getQuery() != null && !dto.getQuery().isBlank()) {

                String normalizedQuery = Normalizer.normalize(dto.getQuery(), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .toLowerCase();

                String[] keywords = normalizedQuery.split("\\s+");
                List<Predicate> namePredicates = new ArrayList<>();

                Join<Discipulado, Member> memberJoin = root.join("member", JoinType.LEFT);

                for (String keyword : keywords) {

                    Expression<String> normalizedName = cb.function(
                            "unaccent", String.class,
                            cb.lower(memberJoin.get("completeName"))
                    );

                    String like = "%" + keyword + "%";
                    namePredicates.add(cb.like(normalizedName, like));
                }

                Predicate namePredicate = cb.and(namePredicates.toArray(new Predicate[0]));

                String likeFull = "%" + normalizedQuery + "%";

                Predicate otherFields = cb.or(
                        cb.like(memberJoin.get("cc"), likeFull),
                        cb.like(memberJoin.get("cellphone"), likeFull)
                );

                predicates.add(cb.or(namePredicate, otherFields));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}