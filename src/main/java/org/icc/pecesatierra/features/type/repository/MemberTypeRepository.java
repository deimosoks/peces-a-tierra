package org.icc.pecesatierra.features.type.repository;

import org.icc.pecesatierra.features.type.MemberType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberTypeRepository extends JpaRepository<MemberType, String> {
    boolean existsByName(String name);
}
