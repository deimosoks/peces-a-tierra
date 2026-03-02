package org.icc.pecesatierra.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.entities.CategoryRules;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.repositories.CategoryRulesRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberCategoryDeterminer {

    private final CategoryRulesRepository categoryRulesRepository;
    private final MemberRepository memberRepository;

    public void determineCategory(Member member) {
        if (member.getBirthdate() == null) {
            log.warn("El miembro {} no tiene fecha de nacimiento, no se puede determinar categoría", member.getId());
            return;
        }

        if (member.isCategoryLocked()) {
            log.info("Miembro {} tiene categoría bloqueada, no se asigna automáticamente", member.getId());
            return;
        }

        int age = Period.between(member.getBirthdate(), LocalDate.now()).getYears();

        List<CategoryRules> rules = categoryRulesRepository.findByActiveTrueOrderByPriorityAsc();

        CategoryRules matchedRule = rules.stream()
                .filter(r -> {

                    if (r.getGender() != null && !r.getGender().equalsIgnoreCase(member.getGender())) {
                        return false;
                    }

                    if (r.getMinAge() != null && age < r.getMinAge()) {
                        return false;
                    }

                    if (r.getMaxAge() != null && age > r.getMaxAge()) {
                        return false;
                    }

                    return true;
                })
                .findFirst()
                .orElse(null);

        if (matchedRule != null) {
            member.setCategoryId(matchedRule.getMemberCategory());
            member.setSubcategoryId(matchedRule.getMemberSubCategory());
            memberRepository.save(member);

            log.info("Miembro {} asignado a categoría '{}' y subcategoría '{}', edad={}",
                    member.getCompleteName(),
                    matchedRule.getMemberCategory().getName(),
                    matchedRule.getMemberSubCategory() != null ? matchedRule.getMemberSubCategory().getName() : "N/A",
                    age);
        } else {
            log.warn("No se encontró regla para el miembro {} con edad {} y género {}", member.getId(), age, member.getGender());
        }
    }
}