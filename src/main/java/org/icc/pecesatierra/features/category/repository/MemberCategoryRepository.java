package org.icc.pecesatierra.features.category.repository;

import org.icc.pecesatierra.features.category.MemberCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberCategoryRepository extends JpaRepository<MemberCategory, String> {
    boolean existsByName(String name);

    /**
     * H-42b: excluye la propia categoria para que guardar sin cambiar el nombre
     * no se considere un conflicto. Mismo criterio que en tipo y subcategoria.
     */
    boolean existsByNameAndIdNot(String name, String id);

    @Override
    @EntityGraph(attributePaths = {
            "subCategories"
    })
    List<MemberCategory> findAll();
}
