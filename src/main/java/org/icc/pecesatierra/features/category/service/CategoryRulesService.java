package org.icc.pecesatierra.features.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.features.category.dtos.rules.CategoryRulesRequestDto;
import org.icc.pecesatierra.features.category.dtos.rules.CategoryRulesResponseDto;
import org.icc.pecesatierra.features.category.CategoryRules;
import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.category.MemberSubCategory;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.category.exceptions.CategoryNotFoundException;
import org.icc.pecesatierra.features.category.exceptions.rules.CategoryRulesNotFoundException;
import org.icc.pecesatierra.features.category.exceptions.rules.SubcategoryDoesNotBelongToCategoryException;
import org.icc.pecesatierra.features.category.exceptions.sub.SubCategoryNotFoundException;
import org.icc.pecesatierra.features.category.repository.CategoryRulesRepository;
import org.icc.pecesatierra.features.category.repository.MemberCategoryRepository;
import org.icc.pecesatierra.features.category.repository.MemberSubCategoryRepository;
import org.icc.pecesatierra.features.category.mapper.CategoryRulesMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryRulesService {

    private final CategoryRulesRepository categoryRulesRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberSubCategoryRepository memberSubCategoryRepository;
    private final CategoryRulesMapper categoryRulesMapper;

    @Transactional
    public CategoryRulesResponseDto create(CategoryRulesRequestDto categoryRulesRequestDto, User user) {

        MemberCategory memberCategory = memberCategoryRepository.findById(categoryRulesRequestDto.getMemberCategoryId())
                .orElseThrow(CategoryNotFoundException::new);

        MemberSubCategory memberSubCategory = categoryRulesRequestDto.getSubCategoryId() != null ? memberSubCategoryRepository.findById(categoryRulesRequestDto.getSubCategoryId())
                .orElseThrow(SubCategoryNotFoundException::new) : null;

        if (memberSubCategory != null && !memberSubCategory.getCategory().getId().equals(memberCategory.getId()))
            throw new SubcategoryDoesNotBelongToCategoryException(memberSubCategory.getName(), memberCategory.getName());

        CategoryRules categoryRules = CategoryRules.builder()
                .minAge(categoryRulesRequestDto.getMinAge())
                .maxAge(categoryRulesRequestDto.getMaxAge())
                .gender(categoryRulesRequestDto.getGender() != null ? categoryRulesRequestDto.getGender().toString() : null)
                .priority(categoryRulesRequestDto.getPriority())
                .memberCategory(memberCategory)
                .memberSubCategory(memberSubCategory)
                .active(true)
                .build();

        return categoryRulesMapper.toDto(categoryRulesRepository.save(categoryRules));
    }

    @Transactional
    public CategoryRulesResponseDto update(CategoryRulesRequestDto categoryRulesRequestDto, User user, String categoryRulesId) {

        CategoryRules categoryRules = categoryRulesRepository.findById(categoryRulesId)
                .orElseThrow(CategoryRulesNotFoundException::new);

        MemberCategory memberCategory = memberCategoryRepository.findById(categoryRulesRequestDto.getMemberCategoryId())
                .orElseThrow(CategoryNotFoundException::new);

        MemberSubCategory memberSubCategory = categoryRulesRequestDto.getSubCategoryId() != null ? memberSubCategoryRepository.findById(categoryRulesRequestDto.getSubCategoryId())
                .orElseThrow(SubCategoryNotFoundException::new) : null;

        if (memberSubCategory != null && !memberSubCategory.getCategory().getId().equals(memberCategory.getId()))
            throw new SubcategoryDoesNotBelongToCategoryException(memberSubCategory.getName(), memberCategory.getName());

        categoryRulesMapper.updateEntityFromDto(categoryRulesRequestDto, categoryRules, memberCategory, memberSubCategory);

        return categoryRulesMapper.toDto(categoryRules);
    }

    @Transactional(readOnly = true)
    public List<CategoryRulesResponseDto> findAll() {
        return categoryRulesRepository.findAll().stream().map(categoryRulesMapper::toDto).toList();
    }

    @Transactional
    public void delete(String categoryRulesId, User user) {
        CategoryRules categoryRules = categoryRulesRepository.findById(categoryRulesId)
                .orElseThrow(CategoryRulesNotFoundException::new);

        categoryRulesRepository.delete(categoryRules);
    }

    @Transactional
    public boolean updateActive(String categoryRulesId, User user, boolean state) {
        CategoryRules categoryRules = categoryRulesRepository.findById(categoryRulesId)
                .orElseThrow(CategoryRulesNotFoundException::new);

        categoryRules.setActive(state);

        return categoryRules.isActive();
    }
}
