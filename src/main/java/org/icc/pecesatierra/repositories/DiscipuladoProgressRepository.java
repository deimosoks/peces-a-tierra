package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.DiscipuladoProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscipuladoProgressRepository extends JpaRepository<DiscipuladoProgress, String>, JpaSpecificationExecutor<DiscipuladoProgressRepository> {
}
