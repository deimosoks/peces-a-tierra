package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.Baptism;
import org.icc.pecesatierra.entities.Branch;
import org.icc.pecesatierra.entities.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaptismRepository extends JpaRepository<Baptism, String> {
    boolean existsByBaptizedMemberAndInvalidFalse(Member member);

    long countByBaptizedMemberBranch(Branch branch);
}
