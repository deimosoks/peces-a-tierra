package org.icc.pecesatierra.utils.mappers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.member.category.MemberCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.category.rules.CategoryRulesRequestDto;
import org.icc.pecesatierra.dtos.member.category.rules.CategoryRulesResponseDto;
import org.icc.pecesatierra.entities.CategoryRules;
import org.icc.pecesatierra.entities.MemberCategory;
import org.icc.pecesatierra.entities.MemberSubCategory;
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
