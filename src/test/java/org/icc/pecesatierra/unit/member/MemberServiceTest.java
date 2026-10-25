package org.icc.pecesatierra.unit.member;

import org.icc.pecesatierra.features.attendance.repository.AttendanceRepository;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.category.repository.mapper.MemberMapper;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.dtos.MemberRequestDto;
import org.icc.pecesatierra.features.member.exceptions.CannotUpdateMemberOutsideYourBranchException;
import org.icc.pecesatierra.features.member.exceptions.MemberNoHasCategoryForThisSubCategoryException;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.member.service.MemberPersistenceService;
import org.icc.pecesatierra.features.member.service.MemberService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.cloudinary.PictureUtils;
import org.icc.pecesatierra.utils.specs.MemberSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

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
    @Mock
    private MemberSpecification memberSpecification;

    @InjectMocks
    private MemberService memberService;

    private final Branch branchA = TestFixtures.branch("branch-a", "Sede A");

    @Test
    void deleteSendsCloudinaryPublicIdInsteadOfUrl_H31() {
        Member member = TestFixtures.member("member-1", branchA, "Ana Perez");
        member.setPictureProfileUrl("https://res.cloudinary.com/demo/image/upload/v1/members/photos/abc123.jpg");
        member.setPublicId("members/photos/abc123");
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(attendanceRepository.existsByMember(member)).thenReturn(false);

        memberService.delete(member.getId(), user);

        verify(memberRepository).delete(member);
        verify(pictureUtils).delete("members/photos/abc123");
        verify(pictureUtils, never())
                .delete("https://res.cloudinary.com/demo/image/upload/v1/members/photos/abc123.jpg");
    }

    @Test
    void deleteOnMemberFromAnotherBranchIsRejectedBeforeAttendanceLookup_M03() {
        Branch branchB = TestFixtures.branch("branch-b", "Sede B");
        Member member = TestFixtures.member("member-1", branchB, "Ana Perez");
        member.setPublicId("members/photos/abc123");
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.delete(member.getId(), user))
                .isInstanceOfSatisfying(CannotUpdateMemberOutsideYourBranchException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        // La sede corta antes de revelar si el integrante ajeno tiene asistencias.
        verify(attendanceRepository, never()).existsByMember(any(Member.class));
        verify(memberRepository, never()).delete(any(Member.class));
        verifyNoInteractions(pictureUtils);
    }

    @Test
    void updateOfForeignMemberChecksBranchBeforeCcUniquenessAndPhoto_M03() {
        Branch branchB = TestFixtures.branch("branch-b", "Sede B");
        Member member = TestFixtures.member("member-1", branchB, "Ana Perez B");
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));
        MemberRequestDto request = MemberRequestDto.builder()
                .cc("999")
                .pictureProfile(mock(MultipartFile.class))
                .build();

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.update(request, member.getId(), user))
                .isInstanceOfSatisfying(CannotUpdateMemberOutsideYourBranchException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).doesNotContain("Ana Perez B");
                });

        // Ni se consulta la CC ajena ni se sube la foto antes de rechazar la sede.
        verify(memberRepository, never()).existsByCc(any());
        verifyNoInteractions(pictureUtils, memberPersistenceService);
    }

    @Test
    void updateActiveOnMemberFromAnotherBranchIsRejected_M03() {
        Branch branchB = TestFixtures.branch("branch-b", "Sede B");
        Member member = TestFixtures.member("member-1", branchB, "Ana Perez");
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.updateActive(member.getId(), false, user))
                .isInstanceOfSatisfying(CannotUpdateMemberOutsideYourBranchException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        assertThat(member.isActive()).isTrue();
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void createWithoutPicturePropagatesTheBusinessException_H23() {
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));
        MemberRequestDto request = MemberRequestDto.builder()
                .completeName("Ana Perez")
                .categoryId("categoria-inexistente")
                .build();
        MemberNoHasCategoryForThisSubCategoryException businessException =
                new MemberNoHasCategoryForThisSubCategoryException("Ana Perez");

        when(memberPersistenceService.save(any(MemberRequestDto.class), isNull(), any(User.class)))
                .thenThrow(businessException);

        // El alta sin foto deja pictureData en null: el catch no debe desreferenciarlo.
        assertThatThrownBy(() -> memberService.create(request, user))
                .isSameAs(businessException)
                .isNotInstanceOf(NullPointerException.class);

        verifyNoInteractions(pictureUtils);
    }

    @Test
    void deleteWithoutPublicIdDoesNotCallCloudinary() {
        Member member = TestFixtures.member("member-1", branchA, "Ana Perez");
        member.setPictureProfileUrl("https://res.cloudinary.com/demo/image/upload/v1/legacy.jpg");
        member.setPublicId(null);
        User admin = TestFixtures.user("admin-1", "admin-a",
                TestFixtures.member("admin-member-1", branchA, "Admin A"), "ADMINISTRATOR");

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(attendanceRepository.existsByMember(member)).thenReturn(false);

        memberService.delete(member.getId(), admin);

        verify(memberRepository).delete(member);
        verifyNoInteractions(pictureUtils);
    }
}
