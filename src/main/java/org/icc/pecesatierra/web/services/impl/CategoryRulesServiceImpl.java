package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.member.category.rules.CategoryRulesRequestDto;
import org.icc.pecesatierra.dtos.member.category.rules.CategoryRulesResponseDto;
import org.icc.pecesatierra.entities.CategoryRules;
import org.icc.pecesatierra.entities.MemberCategory;
import org.icc.pecesatierra.entities.MemberSubCategory;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.members.AlreadyExistsMemberWithCc;
import org.icc.pecesatierra.exceptions.members.categories.CategoryNotFoundException;
import org.icc.pecesatierra.exceptions.members.categories.rules.CategoryRulesNotFoundException;
import org.icc.pecesatierra.exceptions.members.categories.rules.SubcategoryDoesNotBelongToCategoryException;
import org.icc.pecesatierra.exceptions.members.categories.sub.SubCategoryNotFoundException;
import org.icc.pecesatierra.repositories.CategoryRulesRepository;
import org.icc.pecesatierra.repositories.MemberCategoryRepository;
import org.icc.pecesatierra.repositories.MemberSubCategoryRepository;
import org.icc.pecesatierra.utils.mappers.CategoryRulesMapper;
import org.icc.pecesatierra.web.services.CategoryRulesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryRulesServiceImpl implements CategoryRulesService {

    private final CategoryRulesRepository categoryRulesRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberSubCategoryRepository memberSubCategoryRepository;
    private final CategoryRulesMapper categoryRulesMapper;

    @Transactional
    @Override
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
                .gender(categoryRulesRequestDto.getGender() != null? categoryRulesRequestDto.getGender().toString() :null)
                .priority(categoryRulesRequestDto.getPriority())
                .memberCategory(memberCategory)
                .memberSubCategory(memberSubCategory)
                .active(true)
                .build();

        return categoryRulesMapper.toDto(categoryRulesRepository.save(categoryRules));
    }

    @Transactional
    @Override
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
    @Override
    public List<CategoryRulesResponseDto> findAll() {
        return categoryRulesRepository.findAll().stream().map(categoryRulesMapper::toDto).toList();
    }

    @Transactional
    @Override
    public void delete(String categoryRulesId, User user) {
        CategoryRules categoryRules = categoryRulesRepository.findById(categoryRulesId)
                .orElseThrow(CategoryRulesNotFoundException::new);

        categoryRulesRepository.delete(categoryRules);
    }

    @Transactional
    @Override
    public boolean updateActive(String categoryRulesId, User user, boolean state) {
        CategoryRules categoryRules = categoryRulesRepository.findById(categoryRulesId)
                .orElseThrow(CategoryRulesNotFoundException::new);

        categoryRules.setActive(state);

        return categoryRules.isActive();
    }
}
