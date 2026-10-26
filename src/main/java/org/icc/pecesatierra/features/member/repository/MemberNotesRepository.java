package org.icc.pecesatierra.features.member.repository;

import org.icc.pecesatierra.features.member.MemberNotes;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MemberNotesRepository extends JpaRepository<MemberNotes, String> {

    List<MemberNotes> findByMember_IdOrderByCreatedAtDesc(String memberId);

    /**
     * H-39: carga en una sola consulta todas las notas de los integrantes de la
     * pagina. Se trae tambien el autor ({@code createdBy}) porque
     * {@code MemberNotesMapper} lo necesita para el DTO: sin el grafo seria una
     * consulta por nota.
     */
    @EntityGraph(attributePaths = {"createdBy"})
    List<MemberNotes> findByMember_IdIn(Collection<String> memberIds);
}
