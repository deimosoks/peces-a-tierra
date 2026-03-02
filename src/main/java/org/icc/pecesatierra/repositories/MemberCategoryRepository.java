package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.MemberCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberCategoryRepository extends JpaRepository<MemberCategory, String> {
    boolean existsByName(String name);
}
