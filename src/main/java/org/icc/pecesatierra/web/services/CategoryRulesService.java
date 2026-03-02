package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.member.category.rules.CategoryRulesRequestDto;
import org.icc.pecesatierra.dtos.member.category.rules.CategoryRulesResponseDto;
import org.icc.pecesatierra.entities.User;

import java.util.List;

public interface CategoryRulesService {

    CategoryRulesResponseDto create(CategoryRulesRequestDto categoryRulesResponseDto, User user);

    CategoryRulesResponseDto update(CategoryRulesRequestDto categoryRulesRequestDto, User user, String categoryRulesId);

    List<CategoryRulesResponseDto> findAll();

    void delete(String categoryRulesId, User user);

    boolean updateActive(String categoryRuleId, User user, boolean state);

}
