package org.icc.pecesatierra.unit.baptism;

import org.icc.pecesatierra.features.baptism.Baptism;
import org.icc.pecesatierra.features.baptism.dtos.BaptismInvalidRequestDto;
import org.icc.pecesatierra.features.baptism.dtos.BaptismRequestDto;
import org.icc.pecesatierra.features.baptism.dtos.BaptismResponseDto;
import org.icc.pecesatierra.features.baptism.exceptions.CannotInvalidBaptismOutsideYourBranch;
import org.icc.pecesatierra.features.baptism.exceptions.CannotRegisterBaptismWithMemberOutsideYourBranch;
import org.icc.pecesatierra.features.baptism.mapper.BaptismMapper;
import org.icc.pecesatierra.features.baptism.repository.BaptismRepository;
import org.icc.pecesatierra.features.baptism.service.BaptismService;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.exceptions.MemberAlreadyHasRegisteredBaptismActiveException;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.specs.BaptismSpecification;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaptismServiceTest {

    @Mock
    private BaptismRepository baptismRepository;
    @Mock
    private MemberRepository memberRepository;
    @Spy
    private BaptismMapper baptismMapper = new BaptismMapper(new DateTimeUtils());
    @Mock
    private BaptismSpecification baptismSpecification;
    @Mock
    private DateTimeUtils dateTimeUtils;

    @InjectMocks
    private BaptismService baptismService;

    private final Branch branchA = TestFixtures.branch("branch-a", "Sede A");

    @Test
    void persistsNoteAndMunicipalityFromTheRequest_H07() {
        Member baptized = TestFixtures.member("member-1", branchA, "Ana Perez");
        Member operator = TestFixtures.member("operator-1", branchA, "Operador A");
        User user = TestFixtures.user("user-1", "operador-a", operator);
        BaptismRequestDto request = BaptismRequestDto.builder()
                .memberId(baptized.getId())
                .date(LocalDate.of(2026, 1, 15))
                .note("Nota pastoral")
                .address("Calle 10")
                .neighborhood("Centro")
                .city("Bogota")
                .municipality("Medellin")
                .district("Chapinero")
                .postalCode("110111")
                .latitude("4.6")
                .longitude("-74.0")
                .build();

        when(memberRepository.findById(baptized.getId())).thenReturn(Optional.of(baptized));
        when(baptismRepository.existsByBaptizedMemberAndInvalidFalse(baptized)).thenReturn(false);
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(baptismRepository.save(any(Baptism.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BaptismResponseDto response = baptismService.create(request, user);

        ArgumentCaptor<Baptism> captor = ArgumentCaptor.forClass(Baptism.class);
        verify(baptismRepository).save(captor.capture());
        Baptism saved = captor.getValue();

        assertThat(saved.getNote()).isEqualTo("Nota pastoral");
        assertThat(saved.getMunicipality()).isEqualTo("Medellin");
        assertThat(saved.getCity()).isEqualTo("Bogota");
        assertThat(saved.getNeighborhood()).isEqualTo("Centro");
        assertThat(saved.getRegisteredBy()).isSameAs(operator);
        assertThat(saved.getCreatedAt()).isEqualTo(TestFixtures.FIXED_NOW);

        // El DTO lo construye ahora el BaptismMapper real, no un stub.
        assertThat(response.getNote()).isEqualTo("Nota pastoral");
        assertThat(response.getMemberName()).isEqualTo("Ana Perez");
        assertThat(response.getBranchName()).isEqualTo("Sede A");
        assertThat(response.getRegisteredBy()).isEqualTo("Operador A");
    }

    @Test
    void twoBranchInstancesWithTheSameIdAreTheSameBranch_H08() {
        // Mismo id, nombre distinto: si la produccion comparase nombres, este test falla.
        Branch userBranch = TestFixtures.branch("branch-a", "Sede A");
        Branch memberBranch = TestFixtures.branch("branch-a", "Sede A - copia");
        Member baptized = TestFixtures.member("member-1", memberBranch, "Ana Perez");
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", userBranch, "Operador A"));
        BaptismRequestDto request = BaptismRequestDto.builder()
                .memberId(baptized.getId())
                .date(LocalDate.of(2026, 1, 15))
                .note("Nota")
                .build();

        // El defecto H-08 era comparar con Branch.equals: dos instancias distintas
        // con el mismo id se trataban como sedes diferentes.
        assertThat(user.getMember().getBranch()).isNotSameAs(baptized.getBranch());
        assertThat(user.getMember().getBranch().getId()).isEqualTo(baptized.getBranch().getId());

        when(memberRepository.findById(baptized.getId())).thenReturn(Optional.of(baptized));
        when(baptismRepository.existsByBaptizedMemberAndInvalidFalse(baptized)).thenReturn(false);
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(baptismRepository.save(any(Baptism.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> baptismService.create(request, user)).doesNotThrowAnyException();
        verify(baptismRepository).save(any(Baptism.class));
    }

    @Test
    void rejectsBaptismForMemberFromAnotherBranch() {
        Branch branchB = TestFixtures.branch("branch-b", "Sede B");
        Member baptized = TestFixtures.member("member-1", branchB, "Ana Perez");
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));
        BaptismRequestDto request = BaptismRequestDto.builder()
                .memberId(baptized.getId())
                .date(LocalDate.of(2026, 1, 15))
                .build();

        when(memberRepository.findById(baptized.getId())).thenReturn(Optional.of(baptized));

        assertThatThrownBy(() -> baptismService.create(request, user))
                .isInstanceOfSatisfying(CannotRegisterBaptismWithMemberOutsideYourBranch.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(baptismRepository, never()).save(any(Baptism.class));
    }

    @Test
    void createForMemberOfAnotherBranchWithActiveBaptismDoesNotLeakTheName_M02() {
        Branch branchB = TestFixtures.branch("branch-b", "Sede B");
        Member baptized = TestFixtures.member("member-1", branchB, "Ana Perez B");
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));
        BaptismRequestDto request = BaptismRequestDto.builder()
                .memberId(baptized.getId())
                .date(LocalDate.of(2026, 1, 15))
                .build();

        when(memberRepository.findById(baptized.getId())).thenReturn(Optional.of(baptized));
        // El bautismo activo existe, pero un no-admin de otra sede no debe saberlo.
        lenient().when(baptismRepository.existsByBaptizedMemberAndInvalidFalse(baptized)).thenReturn(true);

        assertThatThrownBy(() -> baptismService.create(request, user))
                .isInstanceOfSatisfying(CannotRegisterBaptismWithMemberOutsideYourBranch.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).doesNotContain("Ana Perez B");
                });

        // La comprobacion de sede corta antes de consultar el duplicado.
        verify(baptismRepository, never()).existsByBaptizedMemberAndInvalidFalse(any(Member.class));
        verify(baptismRepository, never()).save(any(Baptism.class));
    }

    @Test
    void administratorCanRegisterBaptismForMemberFromAnotherBranch() {
        Branch branchB = TestFixtures.branch("branch-b", "Sede B");
        Member baptized = TestFixtures.member("member-1", branchB, "Ana Perez");
        User admin = TestFixtures.user("admin-1", "admin-a",
                TestFixtures.member("admin-member-1", branchA, "Admin A"), "ADMINISTRATOR");
        BaptismRequestDto request = BaptismRequestDto.builder()
                .memberId(baptized.getId())
                .date(LocalDate.of(2026, 1, 15))
                .build();

        when(memberRepository.findById(baptized.getId())).thenReturn(Optional.of(baptized));
        when(baptismRepository.existsByBaptizedMemberAndInvalidFalse(baptized)).thenReturn(false);
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(baptismRepository.save(any(Baptism.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> baptismService.create(request, admin)).doesNotThrowAnyException();
        verify(baptismRepository).save(any(Baptism.class));
    }

    @Test
    void createRejectsMemberWithAnActiveBaptism_H08() {
        Member baptized = TestFixtures.member("member-1", branchA, "Ana Perez");
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));
        BaptismRequestDto request = BaptismRequestDto.builder()
                .memberId(baptized.getId())
                .date(LocalDate.of(2026, 1, 15))
                .build();

        when(memberRepository.findById(baptized.getId())).thenReturn(Optional.of(baptized));
        when(baptismRepository.existsByBaptizedMemberAndInvalidFalse(baptized)).thenReturn(true);

        assertThatThrownBy(() -> baptismService.create(request, user))
                .isInstanceOfSatisfying(MemberAlreadyHasRegisteredBaptismActiveException.class,
                        exception -> {
                            assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                            // Dentro de la misma sede el mensaje sigue siendo util.
                            assertThat(exception.getMessage()).contains("Ana Perez");
                        });

        verify(baptismRepository, never()).save(any(Baptism.class));
        verify(baptismMapper, never()).toDto(any(Baptism.class));
    }

    @Test
    void invalidRejectsBaptismForMemberFromAnotherBranch_H08() {
        Branch branchB = TestFixtures.branch("branch-b", "Sede B");
        Member baptized = TestFixtures.member("member-1", branchB, "Ana Perez");
        Baptism baptism = baptismOf(baptized);
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));
        BaptismInvalidRequestDto request = invalidationRequest(baptism);

        when(baptismRepository.findById(baptism.getId())).thenReturn(Optional.of(baptism));

        assertThatThrownBy(() -> baptismService.invalid(request, user))
                .isInstanceOfSatisfying(CannotInvalidBaptismOutsideYourBranch.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        // La rama protegida no debe mutar el bautismo antes de lanzar.
        assertThat(baptism.isInvalid()).isFalse();
        assertThat(baptism.getInvalidatorId()).isNull();
        assertThat(baptism.getInvalidReason()).isNull();
        assertThat(baptism.getInvalidAt()).isNull();
        verify(baptismMapper, never()).toDto(any(Baptism.class));
    }

    @Test
    void invalidAllowsTwoBranchInstancesWithTheSameId_H08() {
        // Mismo id, nombre distinto: si la produccion comparase nombres, este test falla.
        Branch userBranch = TestFixtures.branch("branch-a", "Sede A");
        Branch baptismBranch = TestFixtures.branch("branch-a", "Sede A - copia");
        Member baptized = TestFixtures.member("member-1", baptismBranch, "Ana Perez");
        Baptism baptism = baptismOf(baptized);
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", userBranch, "Operador A"));
        BaptismInvalidRequestDto request = invalidationRequest(baptism);

        assertThat(user.getMember().getBranch()).isNotSameAs(baptism.getBaptizedMember().getBranch());
        assertThat(user.getMember().getBranch().getId())
                .isEqualTo(baptism.getBaptizedMember().getBranch().getId());

        when(baptismRepository.findById(baptism.getId())).thenReturn(Optional.of(baptism));
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);

        assertThatCode(() -> baptismService.invalid(request, user)).doesNotThrowAnyException();

        assertThat(baptism.isInvalid()).isTrue();
        assertThat(baptism.getInvalidatorId()).isSameAs(user.getMember());
    }

    @Test
    void administratorCanInvalidateBaptismForMemberFromAnotherBranch_H08() {
        Branch branchB = TestFixtures.branch("branch-b", "Sede B");
        Member baptized = TestFixtures.member("member-1", branchB, "Ana Perez");
        Baptism baptism = baptismOf(baptized);
        User admin = TestFixtures.user("admin-1", "admin-a",
                TestFixtures.member("admin-member-1", branchA, "Admin A"), "ADMINISTRATOR");
        BaptismInvalidRequestDto request = invalidationRequest(baptism);

        when(baptismRepository.findById(baptism.getId())).thenReturn(Optional.of(baptism));
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);

        BaptismResponseDto response = baptismService.invalid(request, admin);

        assertThat(baptism.isInvalid()).isTrue();
        assertThat(baptism.getInvalidatorId()).isSameAs(admin.getMember());
        assertThat(baptism.getInvalidReason()).isEqualTo("Motivo pastoral");
        assertThat(baptism.getInvalidAt()).isEqualTo(TestFixtures.FIXED_NOW);
        assertThat(response.isInvalid()).isTrue();
        assertThat(response.getInvalidReason()).isEqualTo("Motivo pastoral");
        assertThat(response.getInvalidatedBy()).isEqualTo("Admin A");
    }

    private Baptism baptismOf(Member baptized) {
        return Baptism.builder()
                .id("baptism-1")
                .baptizedMember(baptized)
                .date(LocalDate.of(2026, 1, 15))
                .note("Nota pastoral")
                .createdAt(TestFixtures.FIXED_NOW)
                .registeredBy(baptized)
                .invalid(false)
                .build();
    }

    private BaptismInvalidRequestDto invalidationRequest(Baptism baptism) {
        return BaptismInvalidRequestDto.builder()
                .baptismId(baptism.getId())
                .invalidReason("Motivo pastoral")
                .build();
    }
}
