package org.icc.pecesatierra.features.category.repository;

import org.icc.pecesatierra.features.category.CategoryRules;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRulesRepository extends JpaRepository<CategoryRules, String> {

    @Override
    @EntityGraph(attributePaths = {
            "memberCategory",
            "memberSubCategory"
    })
    List<CategoryRules> findAll();

    List<CategoryRules> findByActiveTrueOrderByPriorityAsc();
}
