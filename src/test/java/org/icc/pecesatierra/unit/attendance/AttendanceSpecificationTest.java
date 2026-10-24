package org.icc.pecesatierra.unit.attendance;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.icc.pecesatierra.features.attendance.Attendance;
import org.icc.pecesatierra.features.attendance.dtos.AttendanceFiltersRequestDto;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.service.ServiceEvent;
import org.icc.pecesatierra.features.service.Services;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.specs.AttendanceSpecification;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Prueba de regresion de H-02: el filtro por sede del usuario debe aplicarse
 * siempre que el usuario no sea administrador, tambien cuando el DTO de filtros
 * es nulo o trae una sede ajena.
 *
 * <p>No usa base de datos ni Docker: se mockean el arbol de criterios y los
 * joins, que es suficiente para observar que predicados construye la
 * especificacion. El comportamiento real contra PostgreSQL lo cubre
 * {@code AttendanceExportIT} cuando hay Docker.</p>
 */
@ExtendWith(MockitoExtension.class)
class AttendanceSpecificationTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Root<Attendance> root;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaQuery<?> query;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaBuilder cb;

    @Mock
    private Join<Attendance, ServiceEvent> serviceEventJoin;

    @Mock
    private Join<ServiceEvent, Services> servicesJoin;

    @Mock
    private Join<Attendance, Member> memberJoin;

    @Mock
    private Join<Attendance, Branch> branchJoin;

    @Mock
    private Path<Object> branchIdPath;

    private final Branch branchA = TestFixtures.branch("branch-a", "Sede A");
    private final Branch branchB = TestFixtures.branch("branch-b", "Sede B");

    private AttendanceSpecification specification;

    @BeforeEach
    void setUp() {
        specification = new AttendanceSpecification(new DateTimeUtils());
        doReturn(serviceEventJoin).when(root).join("serviceEvent", JoinType.INNER);
        doReturn(servicesJoin).when(serviceEventJoin).join("services", JoinType.INNER);
        doReturn(memberJoin).when(root).join("member", JoinType.INNER);
        doReturn(branchJoin).when(root).join("branch", JoinType.INNER);
    }

    @Test
    void nullFiltersForNonAdminStillRestrictsToTheUsersBranch_H02() {
        Predicate branchPredicate = mock(Predicate.class);
        doReturn(branchIdPath).when(branchJoin).get("id");
        doReturn(branchPredicate).when(cb).equal(branchIdPath, "branch-a");

        Predicate predicate = specification.build(null, operatorOf(branchA))
                .toPredicate(root, query, cb);

        assertThat(predicate).isNotNull();
        verify(cb).equal(branchIdPath, "branch-a");

        // El predicado de sede debe llegar al AND final, no solo construirse (M-12).
        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        assertThat(captor.getValue()).contains(branchPredicate);
    }

    @Test
    void foreignBranchIdNeverOverridesTheUsersBranchForNonAdmin_H02() {
        Predicate branchPredicate = mock(Predicate.class);
        doReturn(branchIdPath).when(branchJoin).get("id");
        doReturn(branchPredicate).when(cb).equal(branchIdPath, "branch-a");
        AttendanceFiltersRequestDto dto = AttendanceFiltersRequestDto.builder()
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
    void adminWithoutFiltersGetsNoBranchPredicate_H02() {
        User admin = TestFixtures.user("admin-1", "admin-a",
                TestFixtures.member("admin-member-1", branchA, "Admin A"), "ADMINISTRATOR");

        Predicate predicate = specification.build(null, admin).toPredicate(root, query, cb);

        assertThat(predicate).isNotNull();
        verify(branchJoin, never()).get(anyString());
    }

    private User operatorOf(Branch branch) {
        return TestFixtures.user("user-a", "operador-a",
                TestFixtures.member("operator-a", branch, "Operador A"));
    }
}
