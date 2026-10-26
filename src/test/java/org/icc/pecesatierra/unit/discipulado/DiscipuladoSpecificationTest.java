package org.icc.pecesatierra.unit.discipulado;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.discipulado.Discipulado;
import org.icc.pecesatierra.features.discipulado.dtos.DiscipuladoFilterRequestDto;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.specs.DiscipuladoSpecification;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Prueba de regresion de H-27 en discipulado: el filtro de sede del usuario no
 * administrador debe aplicarse tambien cuando el DTO de filtros es nulo, y las
 * fechas que llegan en hora de Colombia deben convertirse a UTC antes de
 * compararse con {@code date_started}.
 *
 * <p>No usa base de datos ni Docker: se mockean el arbol de criterios y los
 * caminos del {@link Root}, igual que en {@code BaptismSpecificationTest}.</p>
 */
@ExtendWith(MockitoExtension.class)
class DiscipuladoSpecificationTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Root<Discipulado> root;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaQuery<?> query;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> memberPath;

    @Mock
    private Path<Object> branchPath;

    @Mock
    private Path<Object> branchIdPath;

    private final Branch branchA = TestFixtures.branch("branch-a", "Sede A");
    private final Branch branchB = TestFixtures.branch("branch-b", "Sede B");

    private DiscipuladoSpecification specification;

    @BeforeEach
    void setUp() {
        specification = new DiscipuladoSpecification(new DateTimeUtils());
        // lenient: el administrador sin filtros no recorre el camino de la sede.
        lenient().doReturn(memberPath).when(root).get("member");
        lenient().doReturn(branchPath).when(memberPath).get("branch");
        lenient().doReturn(branchIdPath).when(branchPath).get("id");
    }

    @Test
    void nullFiltersForNonAdminStillRestrictToTheUsersBranch_H27() {
        Predicate branchPredicate = mock(Predicate.class);
        doReturn(branchPredicate).when(cb).equal(branchIdPath, branchA.getId());

        Predicate predicate = specification.build(null, operatorOf(branchA))
                .toPredicate(root, query, cb);

        assertThat(predicate).isNotNull();
        verify(cb).equal(branchIdPath, branchA.getId());
        verify(cb, never()).equal(branchIdPath, branchB.getId());

        // El predicado de sede debe llegar al AND final, no solo construirse.
        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        assertThat(captor.getValue()).contains(branchPredicate);
    }

    @Test
    void foreignBranchIdNeverOverridesTheUsersBranchForNonAdmin_H27() {
        Predicate branchPredicate = mock(Predicate.class);
        doReturn(branchPredicate).when(cb).equal(branchIdPath, branchA.getId());
        DiscipuladoFilterRequestDto dto = DiscipuladoFilterRequestDto.builder()
                .branchId(branchB.getId())
                .build();

        specification.build(dto, operatorOf(branchA)).toPredicate(root, query, cb);

        verify(cb).equal(branchIdPath, branchA.getId());
        verify(cb, never()).equal(branchIdPath, branchB.getId());
    }

    @Test
    void dateRangeArrivesInBogotaTimeAndIsComparedInUtc_H27() {
        DiscipuladoFilterRequestDto dto = DiscipuladoFilterRequestDto.builder()
                .dateStartedFrom(LocalDateTime.of(2026, 1, 15, 0, 0))
                .dateStartedTo(LocalDateTime.of(2026, 1, 31, 23, 59))
                .build();

        specification.build(dto, operatorOf(branchA)).toPredicate(root, query, cb);

        ArgumentCaptor<OffsetDateTime> fromCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> toCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);

        // Se captura el rango en el AND final para no depender de un tipo de
        // predicado concreto.
        ArgumentCaptor<Predicate[]> predicates = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(predicates.capture());
        assertThat(predicates.getValue()).isNotEmpty();

        verify(cb).between(any(), fromCaptor.capture(), toCaptor.capture());

        // 00:00 y 23:59 en America/Bogota (UTC-5) son 05:00 y 04:59 del dia siguiente en UTC.
        assertThat(fromCaptor.getValue()).isEqualTo(OffsetDateTime.parse("2026-01-15T05:00:00Z"));
        assertThat(toCaptor.getValue()).isEqualTo(OffsetDateTime.parse("2026-02-01T04:59:00Z"));
    }

    @Test
    void singleDateBoundIsAlsoConvertedFromBogota_H27() {
        DiscipuladoFilterRequestDto dto = DiscipuladoFilterRequestDto.builder()
                .dateStartedFrom(LocalDateTime.of(2026, 1, 15, 0, 0))
                .build();

        specification.build(dto, operatorOf(branchA)).toPredicate(root, query, cb);

        ArgumentCaptor<OffsetDateTime> lowerBound = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(cb).greaterThanOrEqualTo(any(), lowerBound.capture());

        assertThat(lowerBound.getValue()).isEqualTo(OffsetDateTime.parse("2026-01-15T05:00:00Z"));
    }

    @Test
    void adminWithoutFiltersGetsNoBranchPredicate_H27() {
        User admin = TestFixtures.user("admin-1", "admin-a",
                TestFixtures.member("admin-member-1", branchA, "Admin A"), "ADMINISTRATOR");

        Predicate predicate = specification.build(null, admin).toPredicate(root, query, cb);

        assertThat(predicate).isNotNull();
        verify(root, never()).get(anyString());
        verify(cb, never()).equal(branchIdPath, branchA.getId());
    }

    @Test
    void adminWithBranchFilterUsesTheRequestedBranch_H27() {
        User admin = TestFixtures.user("admin-1", "admin-a",
                TestFixtures.member("admin-member-1", branchA, "Admin A"), "ADMINISTRATOR");
        DiscipuladoFilterRequestDto dto = DiscipuladoFilterRequestDto.builder()
                .branchId(branchB.getId())
                .build();

        specification.build(dto, admin).toPredicate(root, query, cb);

        verify(cb).equal(branchIdPath, branchB.getId());
        verify(cb, never()).equal(branchIdPath, branchA.getId());
    }

    private User operatorOf(Branch branch) {
        return TestFixtures.user("user-a", "operador-a",
                TestFixtures.member("operator-a", branch, "Operador A"));
    }
}
