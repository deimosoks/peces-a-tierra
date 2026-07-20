package org.icc.pecesatierra.features.category.mapper;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.category.dtos.rules.CategoryRulesRequestDto;
import org.icc.pecesatierra.features.category.dtos.rules.CategoryRulesResponseDto;
import org.icc.pecesatierra.features.category.CategoryRules;
import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.category.MemberSubCategory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryRulesMapper {

    private final MemberCategoryMapper memberCategoryMapper;
    private final MemberSubCategoryMapper memberSubCategoryMapper;

    public CategoryRulesResponseDto toDto(CategoryRules categoryRules) {
        return CategoryRulesResponseDto.builder()
                .id(categoryRules.getId())
                .minAge(categoryRules.getMinAge())
                .maxAge(categoryRules.getMaxAge())
                .gender(categoryRules.getGender())
                .priority(categoryRules.getPriority())
                .category(memberCategoryMapper.toDto(categoryRules.getMemberCategory()))
                .active(categoryRules.isActive())
                .subCategory(categoryRules.getMemberSubCategory() != null ? memberSubCategoryMapper.toDto(categoryRules.getMemberSubCategory()) : null)
                .build();
    }

    public void updateEntityFromDto(CategoryRulesRequestDto rulesRequestDto, CategoryRules categoryRules, MemberCategory memberCategory, MemberSubCategory memberSubCategory) {

        categoryRules.setMinAge(rulesRequestDto.getMinAge());
        categoryRules.setMaxAge(rulesRequestDto.getMaxAge());
        categoryRules.setGender(rulesRequestDto.getGender() != null ? rulesRequestDto.getGender().toString() : null);
        categoryRules.setPriority(rulesRequestDto.getPriority());
        categoryRules.setMemberCategory(memberCategory);
        categoryRules.setMemberSubCategory(memberSubCategory);

    }

}
