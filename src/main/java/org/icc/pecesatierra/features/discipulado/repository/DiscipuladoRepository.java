package org.icc.pecesatierra.features.discipulado.repository;

import org.icc.pecesatierra.features.discipulado.Discipulado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscipuladoRepository extends JpaRepository<Discipulado, String>, JpaSpecificationExecutor<Discipulado> {
}
