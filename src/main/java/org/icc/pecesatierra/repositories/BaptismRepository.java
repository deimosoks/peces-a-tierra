package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.Baptism;
import org.icc.pecesatierra.entities.Branch;
import org.icc.pecesatierra.entities.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BaptismRepository extends JpaRepository<Baptism, String>, JpaSpecificationExecutor<Baptism> {
    boolean existsByBaptizedMemberAndInvalidFalse(Member member);

    long countByInvalidFalse();

    @Override
    @EntityGraph(attributePaths = {"baptizedMember", "baptizedMember.branch"})
    Page<Baptism> findAll(Specification<Baptism> spec, Pageable pageable);

    long countByBaptizedMemberBranchAndInvalidFalse(Branch branch);
}
