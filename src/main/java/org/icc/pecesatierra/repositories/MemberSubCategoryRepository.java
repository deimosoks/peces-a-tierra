package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.MemberCategory;
import org.icc.pecesatierra.entities.MemberSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberSubCategoryRepository extends JpaRepository<MemberSubCategory, String> {

    boolean existsByCategoryAndName(MemberCategory category,String name);
}
