package org.icc.pecesatierra.features.discipulado.repository;

import org.icc.pecesatierra.features.discipulado.DiscipuladoProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscipuladoProgressRepository extends JpaRepository<DiscipuladoProgress, String>, JpaSpecificationExecutor<DiscipuladoProgressRepository> {
}
