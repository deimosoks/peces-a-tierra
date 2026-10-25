package org.icc.pecesatierra.unit.baptism;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.icc.pecesatierra.features.baptism.Baptism;
import org.icc.pecesatierra.features.baptism.dtos.BaptismFilterRequestDto;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.specs.BaptismSpecification;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Prueba de regresion de H-02 en bautismos: el filtro por sede del usuario debe
 * aplicarse siempre que no sea administrador, tambien cuando el DTO de filtros
 * es nulo, y debe comparar el id de la sede y no la instancia de {@code Branch}.
 * Antes el predicado vivia dentro de {@code if (dto != null)} y comparaba
 * {@code baptizedMember.get("branch")} con la entidad.
 *
 * <p>No usa base de datos ni Docker: se mockean el arbol de criterios y el
 * join. El comportamiento real contra PostgreSQL queda fuera de esta suite,
 * igual que en {@code AttendanceSpecificationTest}.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaptismSpecificationTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Root<Baptism> root;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaQuery<?> query;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaBuilder cb;

    @Mock
    private Join<Baptism, Member> baptizedMemberJoin;

    @Mock
    private Path<Object> branchPath;

    @Mock
    private Path<Object> branchIdPath;

    @Mock
    private Path<Object> memberIdPath;

    private final Branch branchA = TestFixtures.branch("branch-a", "Sede A");
    private final Branch branchB = TestFixtures.branch("branch-b", "Sede B");

    private BaptismSpecification specification;

    @BeforeEach
    void setUp() {
        specification = new BaptismSpecification(new DateTimeUtils());
        doReturn(baptizedMemberJoin).when(root).join("baptizedMember", JoinType.INNER);
        lenient().doReturn(branchPath).when(baptizedMemberJoin).get("branch");
        lenient().doReturn(branchIdPath).when(branchPath).get("id");
        lenient().doReturn(memberIdPath).when(baptizedMemberJoin).get("id");
    }

    @Test
    void nullFiltersForNonAdminStillRestrictsToTheUsersBranch_H02() {
        Predicate branchPredicate = mock(Predicate.class);
        doReturn(branchPredicate).when(cb).equal(branchIdPath, "branch-a");

        Predicate predicate = specification.build(null, operatorOf(branchA))
                .toPredicate(root, query, cb);

        assertThat(predicate).isNotNull();
        verify(cb).equal(branchIdPath, "branch-a");

        // El predicado de sede debe llegar al AND final, no solo construirse.
        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        assertThat(captor.getValue()).contains(branchPredicate);
    }

    @Test
    void foreignBranchIdNeverOverridesTheUsersBranchForNonAdmin_H02() {
        Predicate branchPredicate = mock(Predicate.class);
        doReturn(branchPredicate).when(cb).equal(branchIdPath, "branch-a");
        BaptismFilterRequestDto dto = BaptismFilterRequestDto.builder()
                .branchId(branchB.getId())
                .build();

        specification.build(dto, operatorOf(branchA)).toPredicate(root, query, cb);

        verify(cb).equal(branchIdPath, "branch-a");
        verify(cb, never()).equal(branchIdPath, "branch-b");

        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        assertThat(captor.getValue()).contains(branchPredicate);
    }

    @Test
    void dtoWithFiltersForNonAdminAppliesBranchAndMemberFilters_H02() {
        BaptismFilterRequestDto dto = BaptismFilterRequestDto.builder()
                .memberId("member-1")
                .build();

        specification.build(dto, operatorOf(branchA)).toPredicate(root, query, cb);

        verify(cb).equal(branchIdPath, "branch-a");
        verify(cb).equal(memberIdPath, "member-1");
    }

    @Test
    void adminWithoutFiltersGetsNoBranchPredicate_H02() {
        User admin = TestFixtures.user("admin-1", "admin-a",
                TestFixtures.member("admin-member-1", branchA, "Admin A"), "ADMINISTRATOR");

        Predicate predicate = specification.build(null, admin).toPredicate(root, query, cb);

        assertThat(predicate).isNotNull();
        verify(baptizedMemberJoin, never()).get(anyString());
    }

    @Test
    void adminWithBranchFilterUsesTheRequestedBranch_H02() {
        User admin = TestFixtures.user("admin-1", "admin-a",
                TestFixtures.member("admin-member-1", branchA, "Admin A"), "ADMINISTRATOR");
        BaptismFilterRequestDto dto = BaptismFilterRequestDto.builder()
                .branchId(branchB.getId())
                .build();

        specification.build(dto, admin).toPredicate(root, query, cb);

        verify(cb).equal(branchIdPath, "branch-b");
        verify(cb, never()).equal(branchIdPath, "branch-a");
    }

    private User operatorOf(Branch branch) {
        return TestFixtures.user("user-a", "operador-a",
                TestFixtures.member("operator-a", branch, "Operador A"));
    }
}
