package org.icc.pecesatierra.unit.filters;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.icc.pecesatierra.configurations.ExportLimits;
import org.icc.pecesatierra.features.attendance.Attendance;
import org.icc.pecesatierra.features.attendance.mapper.AttendanceMapper;
import org.icc.pecesatierra.features.attendance.repository.AttendanceRepository;
import org.icc.pecesatierra.features.attendance.service.AttendanceService;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.category.repository.mapper.MemberMapper;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.repository.MemberNotesRepository;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.member.service.MemberPersistenceService;
import org.icc.pecesatierra.features.member.service.MemberService;
import org.icc.pecesatierra.features.role.repository.RoleRepository;
import org.icc.pecesatierra.features.service.ServiceEvent;
import org.icc.pecesatierra.features.service.Services;
import org.icc.pecesatierra.features.service.exceptions.InvalidStartAndEndDatesException;
import org.icc.pecesatierra.features.service.mapper.ServiceEventMapper;
import org.icc.pecesatierra.features.service.repository.ServiceEventRepository;
import org.icc.pecesatierra.features.service.repository.ServiceRepository;
import org.icc.pecesatierra.features.service.service.ServiceEventService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.user.dtos.UserFilterRequestDto;
import org.icc.pecesatierra.features.user.mapper.UserMapper;
import org.icc.pecesatierra.features.user.repository.UserRepository;
import org.icc.pecesatierra.features.user.service.UserService;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.cloudinary.PictureUtils;
import org.icc.pecesatierra.utils.specs.AttendanceSpecification;
import org.icc.pecesatierra.utils.specs.MemberSpecification;
import org.icc.pecesatierra.utils.specs.ServiceEventSpecification;
import org.icc.pecesatierra.utils.specs.UserSpecification;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * H-10: los endpoints con {@code @RequestBody(required = false)} deben aceptar un
 * cuerpo nulo. El servicio normaliza el DTO a una instancia vacia y el
 * specification sigue aplicando el filtro de sede del usuario no administrador.
 *
 * <p>Se usa el specification real con un arbol de criterios mockeado: no toca la
 * base de datos y permite observar que predicado se construye.</p>
 */
@ExtendWith(MockitoExtension.class)
class NullFilterBodyTest {

    private final DateTimeUtils dateTimeUtils = new DateTimeUtils();
    private final Branch branchA = TestFixtures.branch("branch-a", "Sede A");
    private final Branch branchB = TestFixtures.branch("branch-b", "Sede B");

    // ---------- MemberService ----------

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberMapper memberMapper;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private MemberPersistenceService memberPersistenceService;
    @Mock
    private PictureUtils pictureUtils;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Root<Member> memberRoot;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaQuery<?> memberQuery;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaBuilder memberCriteriaBuilder;

    private MemberService memberService;

    // ---------- AttendanceService ----------

    @Mock
    private ServiceEventRepository serviceEventRepository;
    @Mock
    private AttendanceMapper attendanceMapper;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Root<Attendance> attendanceRoot;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaQuery<?> attendanceQuery;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaBuilder attendanceCriteriaBuilder;
    @Mock
    private Join<Attendance, ServiceEvent> serviceEventJoin;
    @Mock
    private Join<ServiceEvent, Services> servicesJoin;
    @Mock
    private Join<Attendance, Member> memberJoin;
    @Mock
    private Join<Attendance, Branch> attendanceBranchJoin;

    private AttendanceService attendanceService;

    // ---------- UserService ----------

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Root<User> userRoot;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaQuery<?> userQuery;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaBuilder userCriteriaBuilder;
    @Mock
    private Join<User, Member> userMemberJoin;

    private UserService userService;

    // ---------- ServiceEventService ----------

    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private org.icc.pecesatierra.features.branch.repository.BranchRepository branchRepository;
    @Mock
    private ServiceEventMapper serviceEventMapper;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Root<ServiceEvent> serviceEventRoot;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaQuery<?> serviceEventQuery;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaBuilder serviceEventCriteriaBuilder;
    @Mock
    private Join<ServiceEvent, Services> eventServicesJoin;
    @Mock
    private Join<ServiceEvent, Branch> eventBranchJoin;

    private ServiceEventService serviceEventService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, memberMapper, attendanceRepository,
                memberPersistenceService, pictureUtils, new MemberSpecification(dateTimeUtils),
                new ExportLimits(10000), mock(MemberNotesRepository.class));
        attendanceService = new AttendanceService(attendanceRepository, memberRepository, attendanceMapper,
                serviceEventRepository, new AttendanceSpecification(dateTimeUtils), dateTimeUtils,
                new ExportLimits(10000));
        userService = new UserService(userRepository, memberRepository, passwordEncoder, roleRepository,
                userMapper, new UserSpecification(), dateTimeUtils);
        serviceEventService = new ServiceEventService(serviceEventRepository, serviceEventMapper,
                serviceRepository, branchRepository, attendanceRepository,
                new ServiceEventSpecification(dateTimeUtils), dateTimeUtils);
    }

    @Test
    void memberSearchWithNullBodyUsesEmptyFiltersAndKeepsBranchScoping_H10() {
        when(memberRepository.findAll(anySpecification(), any(Pageable.class))).thenReturn(Page.empty());

        assertThatCode(() -> memberService.search(0, null, operatorOf(branchA)))
                .doesNotThrowAnyException();

        ArgumentCaptor<Specification<Member>> captor = specificationCaptor();
        verify(memberRepository).findAll(captor.capture(), any(Pageable.class));

        Path<Object> branchPath = mock(Path.class);
        Path<Object> branchIdPath = mock(Path.class);
        Predicate branchPredicate = mock(Predicate.class);
        doReturn(branchPath).when(memberRoot).get("branch");
        doReturn(branchIdPath).when(branchPath).get("id");
        doReturn(branchPredicate).when(memberCriteriaBuilder).equal(branchIdPath, branchA.getId());

        captor.getValue().toPredicate(memberRoot, memberQuery, memberCriteriaBuilder);

        verify(memberCriteriaBuilder).equal(branchIdPath, branchA.getId());
        verify(memberCriteriaBuilder, never()).equal(branchIdPath, branchB.getId());

        // M-12: el predicado de sede debe estar en el AND final, no solo construirse.
        ArgumentCaptor<Predicate[]> predicates = ArgumentCaptor.forClass(Predicate[].class);
        verify(memberCriteriaBuilder).and(predicates.capture());
        assertThat(predicates.getValue()).contains(branchPredicate);
    }

    @Test
    void memberSpecificationWithNullDtoStillRestrictsToTheUsersBranch_M17() {
        Path<Object> branchPath = mock(Path.class);
        Path<Object> branchIdPath = mock(Path.class);
        Predicate branchPredicate = mock(Predicate.class);
        doReturn(branchPath).when(memberRoot).get("branch");
        doReturn(branchIdPath).when(branchPath).get("id");
        doReturn(branchPredicate).when(memberCriteriaBuilder).equal(branchIdPath, branchA.getId());

        // build(null, noAdmin) ya no puede devolver cb.conjunction().
        new MemberSpecification(dateTimeUtils).build(null, operatorOf(branchA))
                .toPredicate(memberRoot, memberQuery, memberCriteriaBuilder);

        verify(memberCriteriaBuilder).equal(branchIdPath, branchA.getId());
        ArgumentCaptor<Predicate[]> predicates = ArgumentCaptor.forClass(Predicate[].class);
        verify(memberCriteriaBuilder).and(predicates.capture());
        assertThat(predicates.getValue()).contains(branchPredicate);
    }

    @Test
    void attendanceSearchWithNullBodyUsesEmptyFiltersAndKeepsBranchScoping_H10() {
        when(attendanceRepository.findAll(anySpecification(), any(Pageable.class))).thenReturn(Page.empty());
        doReturn(serviceEventJoin).when(attendanceRoot).join("serviceEvent", JoinType.INNER);
        doReturn(servicesJoin).when(serviceEventJoin).join("services", JoinType.INNER);
        doReturn(memberJoin).when(attendanceRoot).join("member", JoinType.INNER);
        doReturn(attendanceBranchJoin).when(attendanceRoot).join("branch", JoinType.INNER);
        Path<Object> branchIdPath = mock(Path.class);
        Predicate branchPredicate = mock(Predicate.class);
        doReturn(branchIdPath).when(attendanceBranchJoin).get("id");
        doReturn(branchPredicate).when(attendanceCriteriaBuilder).equal(branchIdPath, branchA.getId());

        assertThatCode(() -> attendanceService.search(0, null, operatorOf(branchA)))
                .doesNotThrowAnyException();

        ArgumentCaptor<Specification<Attendance>> captor = specificationCaptor();
        verify(attendanceRepository).findAll(captor.capture(), any(Pageable.class));

        captor.getValue().toPredicate(attendanceRoot, attendanceQuery, attendanceCriteriaBuilder);

        verify(attendanceCriteriaBuilder).equal(branchIdPath, branchA.getId());
        verify(attendanceCriteriaBuilder, never()).equal(branchIdPath, branchB.getId());

        // M-12: el predicado de sede debe estar en el AND final, no solo construirse.
        ArgumentCaptor<Predicate[]> predicates = ArgumentCaptor.forClass(Predicate[].class);
        verify(attendanceCriteriaBuilder).and(predicates.capture());
        assertThat(predicates.getValue()).contains(branchPredicate);
    }

    @Test
    void userSearchWithNullBodyUsesEmptyFiltersAndKeepsBranchScoping_H10() {
        when(userRepository.findAll(anySpecification(), any(Pageable.class))).thenReturn(Page.empty());
        doReturn(userMemberJoin).when(userRoot).join("member", JoinType.INNER);
        Path<Object> memberBranchPath = mock(Path.class);
        Path<Object> branchIdPath = mock(Path.class);
        doReturn(memberBranchPath).when(userMemberJoin).get("branch");
        doReturn(branchIdPath).when(memberBranchPath).get("id");

        assertThatCode(() -> userService.search(0, null, operatorOf(branchA)))
                .doesNotThrowAnyException();

        ArgumentCaptor<Specification<User>> captor = specificationCaptor();
        verify(userRepository).findAll(captor.capture(), any(Pageable.class));

        captor.getValue().toPredicate(userRoot, userQuery, userCriteriaBuilder);

        verify(userCriteriaBuilder).equal(branchIdPath, branchA.getId());
        verify(userCriteriaBuilder, never()).equal(branchIdPath, branchB.getId());
    }

    @Test
    void calendarWithNullBodyReturnsTheBusiness400InsteadOfNpe_H10() {
        assertThatThrownBy(() -> serviceEventService.findForCalendar(null, operatorOf(branchA)))
                .isInstanceOfSatisfying(InvalidStartAndEndDatesException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(serviceEventRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void calendarKeepsBranchScopingForNonAdminWhenDatesArePresent_H10() {
        var dto = org.icc.pecesatierra.features.service.dtos.event.ServiceEventsFilterRequestDto.builder()
                .startDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .endDate(LocalDateTime.of(2026, 1, 31, 23, 59))
                .branchId(branchB.getId())
                .build();
        when(serviceEventRepository.findAll(any(Specification.class))).thenReturn(List.of());
        doReturn(eventServicesJoin).when(serviceEventRoot).join("services", JoinType.INNER);
        doReturn(eventBranchJoin).when(serviceEventRoot).join("branch", JoinType.INNER);
        Path<Object> branchIdPath = mock(Path.class);
        doReturn(branchIdPath).when(eventBranchJoin).get("id");

        serviceEventService.findForCalendar(dto, operatorOf(branchA));

        ArgumentCaptor<Specification<ServiceEvent>> captor = specificationCaptor();
        verify(serviceEventRepository).findAll(captor.capture());

        captor.getValue().toPredicate(serviceEventRoot, serviceEventQuery, serviceEventCriteriaBuilder);

        verify(serviceEventCriteriaBuilder).equal(branchIdPath, branchA.getId());
        verify(serviceEventCriteriaBuilder, never()).equal(branchIdPath, branchB.getId());
    }

    @Test
    void userFilterDtoFromAForeignBranchStillCannotBeUsedByNonAdmin_H10() {
        UserFilterRequestDto dto = UserFilterRequestDto.builder().branchId(branchB.getId()).build();
        when(userRepository.findAll(anySpecification(), any(Pageable.class))).thenReturn(Page.empty());
        doReturn(userMemberJoin).when(userRoot).join("member", JoinType.INNER);
        Path<Object> memberBranchPath = mock(Path.class);
        Path<Object> branchIdPath = mock(Path.class);
        doReturn(memberBranchPath).when(userMemberJoin).get("branch");
        doReturn(branchIdPath).when(memberBranchPath).get("id");

        userService.search(0, dto, operatorOf(branchA));

        ArgumentCaptor<Specification<User>> captor = specificationCaptor();
        verify(userRepository).findAll(captor.capture(), any(Pageable.class));
        captor.getValue().toPredicate(userRoot, userQuery, userCriteriaBuilder);

        verify(userCriteriaBuilder).equal(branchIdPath, branchA.getId());
        verify(userCriteriaBuilder, never()).equal(branchIdPath, branchB.getId());
    }

    private User operatorOf(Branch branch) {
        return TestFixtures.user("user-a", "operador-a",
                TestFixtures.member("operator-a", branch, "Operador A"));
    }

    @SuppressWarnings("unchecked")
    private static <T> Specification<T> anySpecification() {
        return any(Specification.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> ArgumentCaptor<Specification<T>> specificationCaptor() {
        return (ArgumentCaptor<Specification<T>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Specification.class);
    }
}
