package org.icc.pecesatierra.features.attendance.repository;

import org.icc.pecesatierra.features.attendance.Attendance;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.service.ServiceEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, String>, JpaSpecificationExecutor<Attendance> {

    @Override
    @EntityGraph(attributePaths = {
            "memberCategory",
            "memberType",
            "registeredById",
            "memberSubCategory",
            "invalidatorId",
            "branch",
            "serviceEvent",
            "member"
    })
    Page<Attendance> findAll(Specification<Attendance> spec, Pageable pageable);

    /**
     * H-39: esta es la consulta de la exportacion. {@code AttendanceMapper}
     * recorre el integrante, su evento de servicio (con el nombre del servicio),
     * la sede, la categoria, el tipo, la subcategoria, quien registro y quien
     * invalido; todas son to-one, asi que van en el grafo y se resuelven con la
     * propia consulta en vez de una consulta por fila cerca del tope de
     * {@code EXPORT_MAX_ROWS}.
     *
     * <p>Se queda FUERA {@code memberCategory.subCategories}: es una coleccion y
     * unirla sobre una lista de hasta {@code EXPORT_MAX_ROWS} filas multiplicaria
     * el resultado por cada subcategoria, devolviendo asistencias repetidas. El
     * mapper la sigue leyendo de forma perezosa, ahora en lotes de 50 categorias
     * gracias a {@code spring.jpa.properties.hibernate.default_batch_fetch_size},
     * de modo que el numero de consultas depende de las categorias distintas y no
     * del numero de asistencias (mismo criterio que en {@code MemberService.search}
     * y en {@code DiscipuladoRepository}).</p>
     */
    @Override
    @EntityGraph(attributePaths = {
            "member",
            "serviceEvent",
            "serviceEvent.services",
            "branch",
            "memberCategory",
            "memberType",
            "memberSubCategory",
            "registeredById",
            "invalidatorId"
    })
    List<Attendance> findAll(Specification<Attendance> spec);

    boolean existsByMember(Member member);

    boolean existsAttendanceByBranch(Branch branch);

    boolean existsByServiceEvent(ServiceEvent serviceEvent);

    @Query("SELECT a.member.id FROM Attendance a WHERE a.serviceEvent.id = :eventId AND a.invalid = false")
    Set<String> findMemberIdsByServiceEventIdInvalidFalse(String eventId);
}
