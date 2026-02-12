package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.MemberType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberTypeRepository extends JpaRepository<MemberType, String> {
    boolean existsByName(String name);
}
