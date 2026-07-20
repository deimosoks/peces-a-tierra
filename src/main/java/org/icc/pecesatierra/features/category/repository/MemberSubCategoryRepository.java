package org.icc.pecesatierra.features.category.repository;

import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.category.MemberSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberSubCategoryRepository extends JpaRepository<MemberSubCategory, String> {

    boolean existsByCategoryAndName(MemberCategory category,String name);
}
