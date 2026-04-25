package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.Discipulado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscipuladoRepository extends JpaRepository<Discipulado, String>, JpaSpecificationExecutor<Discipulado> {
}
