package org.icc.pecesatierra.features.category.repository;

import org.icc.pecesatierra.features.category.MemberCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberCategoryRepository extends JpaRepository<MemberCategory, String> {
    boolean existsByName(String name);

    @Override
    @EntityGraph(attributePaths = {
            "subCategories"
    })
    List<MemberCategory> findAll();
}
