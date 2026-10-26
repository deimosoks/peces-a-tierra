package org.icc.pecesatierra.features.discipulado.repository;

import org.icc.pecesatierra.features.discipulado.Discipulado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscipuladoRepository extends JpaRepository<Discipulado, String>, JpaSpecificationExecutor<Discipulado> {

    /**
     * H-39: {@code DiscipuladoMapper} recorre {@code member} (con su sede,
     * categoria, tipo y subcategoria), {@code registeredBy} y {@code progress}.
     * Sin grafo, cada fila de la pagina disparaba una consulta por asociacion.
     *
     * <p>{@code progress} es una coleccion y se deja FUERA del grafo a
     * proposito: unirla en la misma consulta paginada multiplicaria las filas
     * (producto cartesiano) y Hibernate tendria que paginar en memoria. Se carga
     * en un unico lote adicional gracias a
     * {@code spring.jpa.properties.hibernate.default_batch_fetch_size=50}, de
     * modo que una pagina de 20 discipulados resuelve sus 13 pasos en una
     * consulta y no en veinte.</p>
     */
    @Override
    @EntityGraph(attributePaths = {
            "member",
            "member.branch",
            "member.categoryId",
            "member.typeId",
            "member.subcategoryId",
            "member.registeredBy",
            "registeredBy"
    })
    Page<Discipulado> findAll(Specification<Discipulado> spec, Pageable pageable);
}
