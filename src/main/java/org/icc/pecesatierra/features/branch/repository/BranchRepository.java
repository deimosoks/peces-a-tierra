package org.icc.pecesatierra.features.branch.repository;

import org.icc.pecesatierra.features.branch.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
}
