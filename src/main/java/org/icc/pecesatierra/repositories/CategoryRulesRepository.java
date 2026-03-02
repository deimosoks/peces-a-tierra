package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.CategoryRules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRulesRepository extends JpaRepository<CategoryRules, String> {

    List<CategoryRules> findByActiveTrueOrderByPriorityAsc();
}
