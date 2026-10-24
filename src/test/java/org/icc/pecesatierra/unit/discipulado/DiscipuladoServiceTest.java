package org.icc.pecesatierra.unit.discipulado;

import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.discipulado.Discipulado;
import org.icc.pecesatierra.features.discipulado.DiscipuladoProgress;
import org.icc.pecesatierra.features.discipulado.dtos.DiscipuladoProgressRequestDto;
import org.icc.pecesatierra.features.discipulado.dtos.DiscipuladoProgressResponseDto;
import org.icc.pecesatierra.features.discipulado.exceptions.CannotAssignTeacherOutsideYourBranch;
import org.icc.pecesatierra.features.discipulado.exceptions.CannotUpdateProgressWithMemberOutsideYourBranch;
import org.icc.pecesatierra.features.discipulado.mapper.DiscipuladoMapper;
import org.icc.pecesatierra.features.discipulado.mapper.DiscipuladoProgressMapper;
import org.icc.pecesatierra.features.discipulado.repository.DiscipuladoProgressRepository;
import org.icc.pecesatierra.features.discipulado.repository.DiscipuladoRepository;
import org.icc.pecesatierra.features.discipulado.service.DiscipuladoService;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.specs.DiscipuladoSpecification;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscipuladoServiceTest {

    @Mock
    private DiscipuladoRepository discipuladoRepository;
    @Mock
    private DiscipuladoMapper discipuladoMapper;
    @Mock
    private DateTimeUtils dateTimeUtils;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private DiscipuladoSpecification discipuladoSpecification;
    @Mock
    private DiscipuladoProgressRepository discipuladoProgressRepository;
    @Mock
    private DiscipuladoProgressMapper discipuladoProgressMapper;

    @InjectMocks
    private DiscipuladoService discipuladoService;

    private final Branch branchA = TestFixtures.branch("branch-a", "Sede A");
    private final Branch branchB = TestFixtures.branch("branch-b", "Sede B");
    private final Member memberA = TestFixtures.member("member-a", branchA, "Ana A");
    private final Member memberB = TestFixtures.member("member-b", branchB, "Beto B");
    private final User userA = TestFixtures.user("user-a", "operador-a",
            TestFixtures.member("operator-a", branchA, "Operador A"));
    private final User admin = TestFixtures.user("admin-1", "admin-a",
            TestFixtures.member("admin-member-1", branchA, "Admin A"), "ADMINISTRATOR");

    @Test
    void progressUpdatesCompletedStepForMemberOfTheSameBranch() {
        DiscipuladoProgress progress = progressOf(memberA, memberA);
        Member teacher = TestFixtures.member("teacher-a", branchA, "Maestro A");
        DiscipuladoProgressRequestDto request = progressRequest(teacher.getId());
        OffsetDateTime expectedDate = OffsetDateTime.parse("2026-01-15T01:00:00Z");

        when(discipuladoProgressRepository.findById(progress.getId())).thenReturn(Optional.of(progress));
        when(memberRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(dateTimeUtils.toUTC(request.getDateCompleted())).thenReturn(expectedDate);
        when(discipuladoProgressMapper.toDto(any(DiscipuladoProgress.class))).thenReturn(
                DiscipuladoProgressResponseDto.builder().id(progress.getId()).completed(true).build());

        DiscipuladoProgressResponseDto response = discipuladoService.progress(request, userA);

        assertThat(progress.isCompleted()).isTrue();
        assertThat(progress.getTeacherId()).isSameAs(teacher);
        assertThat(progress.getDateCompleted()).isEqualTo(expectedDate);
        assertThat(response.isCompleted()).isTrue();
    }

    @Test
    void rejectsProgressForMemberFromAnotherBranch_H03() {
        DiscipuladoProgress progress = progressOf(memberB, memberB);
        Member teacher = TestFixtures.member("teacher-b", branchB, "Maestro B");
        DiscipuladoProgressRequestDto request = progressRequest(teacher.getId());

        when(discipuladoProgressRepository.findById(progress.getId())).thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> discipuladoService.progress(request, userA))
                .isInstanceOfSatisfying(CannotUpdateProgressWithMemberOutsideYourBranch.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        assertThat(progress.isCompleted()).isFalse();
        assertThat(progress.getTeacherId()).isNull();
        assertThat(progress.getDateCompleted()).isNull();
    }

    @Test
    void rejectsProgressWhenTeacherIsFromAnotherBranch_H03() {
        DiscipuladoProgress progress = progressOf(memberA, memberA);
        Member teacher = TestFixtures.member("teacher-b", branchB, "Maestro B");
        DiscipuladoProgressRequestDto request = progressRequest(teacher.getId());

        when(discipuladoProgressRepository.findById(progress.getId())).thenReturn(Optional.of(progress));
        when(memberRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> discipuladoService.progress(request, userA))
                .isInstanceOfSatisfying(CannotAssignTeacherOutsideYourBranch.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        assertThat(progress.isCompleted()).isFalse();
        assertThat(progress.getTeacherId()).isNull();
        assertThat(progress.getDateCompleted()).isNull();
    }

    @Test
    void rejectsUpdateProgressForMemberFromAnotherBranch_H03() {
        Member teacher = TestFixtures.member("teacher-b", branchB, "Maestro B");
        DiscipuladoProgress progress = progressOf(memberB, memberB);
        DiscipuladoProgressRequestDto request = progressRequest(teacher.getId());

        when(discipuladoProgressRepository.findById(progress.getId())).thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> discipuladoService.updateProgress(request, userA, progress.getId()))
                .isInstanceOfSatisfying(CannotUpdateProgressWithMemberOutsideYourBranch.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        assertThat(progress.getTeacherId()).isNull();
        assertThat(progress.getDateCompleted()).isNull();
    }

    @Test
    void rejectsUpdateProgressWhenTeacherIsFromAnotherBranch_H03() {
        Member teacher = TestFixtures.member("teacher-b", branchB, "Maestro B");
        DiscipuladoProgress progress = progressOf(memberA, memberA);
        DiscipuladoProgressRequestDto request = progressRequest(teacher.getId());

        when(discipuladoProgressRepository.findById(progress.getId())).thenReturn(Optional.of(progress));
        when(memberRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> discipuladoService.updateProgress(request, userA, progress.getId()))
                .isInstanceOfSatisfying(CannotAssignTeacherOutsideYourBranch.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        assertThat(progress.getTeacherId()).isNull();
        assertThat(progress.getDateCompleted()).isNull();
    }

    @Test
    void administratorCanCompleteProgressForMemberFromAnotherBranch() {
        DiscipuladoProgress progress = progressOf(memberB, memberB);
        Member teacher = TestFixtures.member("teacher-b", branchB, "Maestro B");
        DiscipuladoProgressRequestDto request = progressRequest(teacher.getId());

        when(discipuladoProgressRepository.findById(progress.getId())).thenReturn(Optional.of(progress));
        when(memberRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(dateTimeUtils.toUTC(request.getDateCompleted())).thenReturn(TestFixtures.FIXED_NOW);
        when(discipuladoProgressMapper.toDto(any(DiscipuladoProgress.class))).thenReturn(
                DiscipuladoProgressResponseDto.builder().id(progress.getId()).completed(true).build());

        DiscipuladoProgressResponseDto response = discipuladoService.progress(request, admin);

        assertThat(progress.isCompleted()).isTrue();
        assertThat(progress.getTeacherId()).isSameAs(teacher);
        assertThat(response.isCompleted()).isTrue();
    }

    @Test
    void administratorCanUpdateProgressForMemberFromAnotherBranch() {
        DiscipuladoProgress progress = progressOf(memberB, memberB);
        Member teacher = TestFixtures.member("teacher-b", branchB, "Maestro B");
        DiscipuladoProgressRequestDto request = progressRequest(teacher.getId());

        when(discipuladoProgressRepository.findById(progress.getId())).thenReturn(Optional.of(progress));
        when(memberRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(dateTimeUtils.toUTC(request.getDateCompleted())).thenReturn(TestFixtures.FIXED_NOW);
        when(discipuladoProgressMapper.toDto(any(DiscipuladoProgress.class))).thenReturn(
                DiscipuladoProgressResponseDto.builder().id(progress.getId()).build());

        discipuladoService.updateProgress(request, admin, progress.getId());

        assertThat(progress.getTeacherId()).isSameAs(teacher);
        assertThat(progress.getDateCompleted()).isEqualTo(TestFixtures.FIXED_NOW);
    }

    private DiscipuladoProgress progressOf(Member member, Member registeredBy) {
        Discipulado discipulado = Discipulado.builder()
                .id("discipulado-1")
                .member(member)
                .registeredBy(registeredBy)
                .dateStarted(TestFixtures.FIXED_NOW.minusMonths(1))
                .createdAt(TestFixtures.FIXED_NOW.minusMonths(1))
                .completed(false)
                .build();
        return DiscipuladoProgress.builder()
                .id("progress-1")
                .discipulado(discipulado)
                .step(1)
                .completed(false)
                .registeredBy(registeredBy)
                .createdAt(TestFixtures.FIXED_NOW.minusMonths(1))
                .build();
    }

    private DiscipuladoProgressRequestDto progressRequest(String teacherId) {
        return DiscipuladoProgressRequestDto.builder()
                .discipuladoProgressId("progress-1")
                .teacherId(teacherId)
                .dateCompleted(LocalDateTime.of(2026, 1, 14, 20, 0))
                .build();
    }
}
