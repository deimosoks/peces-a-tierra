package org.icc.pecesatierra.unit.member;

import org.icc.pecesatierra.features.attendance.repository.AttendanceRepository;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.category.repository.mapper.MemberMapper;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.exceptions.CannotDeleteMemberOutSideYourBranchException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    void deleteOnMemberFromAnotherBranchIsRejected() {
        Branch branchB = TestFixtures.branch("branch-b", "Sede B");
        Member member = TestFixtures.member("member-1", branchB, "Ana Perez");
        member.setPublicId("members/photos/abc123");
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(attendanceRepository.existsByMember(member)).thenReturn(false);

        assertThatThrownBy(() -> memberService.delete(member.getId(), user))
                .isInstanceOfSatisfying(CannotDeleteMemberOutSideYourBranchException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(memberRepository, never()).delete(any(Member.class));
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
