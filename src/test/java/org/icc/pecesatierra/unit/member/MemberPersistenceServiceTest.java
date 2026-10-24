package org.icc.pecesatierra.unit.member;

import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.branch.repository.BranchRepository;
import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.category.repository.MemberCategoryRepository;
import org.icc.pecesatierra.features.category.repository.MemberSubCategoryRepository;
import org.icc.pecesatierra.features.category.repository.mapper.MemberMapper;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.dtos.MemberRequestDto;
import org.icc.pecesatierra.features.member.exceptions.CannotUpdateMemberOutsideYourBranchException;
import org.icc.pecesatierra.features.member.exceptions.MemberNoHasCategoryForThisSubCategoryException;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.member.service.MemberPersistenceService;
import org.icc.pecesatierra.features.type.MemberType;
import org.icc.pecesatierra.features.type.repository.MemberTypeRepository;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.MemberCategoryDeterminer;
import org.icc.pecesatierra.utils.enums.AppPermission;
import org.icc.pecesatierra.utils.enums.Gender;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberPersistenceServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberMapper memberMapper;
    @Mock
    private MemberCategoryRepository memberCategoryRepository;
    @Mock
    private MemberTypeRepository memberTypeRepository;
    @Mock
    private MemberSubCategoryRepository memberSubCategoryRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private MemberCategoryDeterminer memberCategoryDeterminer;
    @Mock
    private DateTimeUtils dateTimeUtils;

    @InjectMocks
    private MemberPersistenceService memberPersistenceService;

    private final Branch branchA = TestFixtures.branch("branch-a", "Sede A");
    private final Branch branchB = TestFixtures.branch("branch-b", "Sede B");

    private Member memberIn(Branch branch, String completeName) {
        return Member.builder()
                .id("member-1")
                .completeName(completeName)
                .cc("1000")
                .cellphone("3001112233")
                .branch(branch)
                .categoryId(category())
                .typeId(type())
                .active(true)
                .createdAt(TestFixtures.FIXED_NOW)
                .build();
    }

    private MemberCategory category() {
        return MemberCategory.builder()
                .id("cat-1")
                .name("Categoria")
                .color("#112233")
                .build();
    }

    private MemberType type() {
        return MemberType.builder()
                .id("type-1")
                .name("Tipo")
                .color("#112233")
                .build();
    }

    private MemberRequestDto request(String completeName, String subCategoryId) {
        MemberRequestDto dto = new MemberRequestDto();
        dto.setCompleteName(completeName);
        dto.setTypeId("type-1");
        dto.setCategoryId("cat-1");
        dto.setSubCategoryId(subCategoryId);
        dto.setCc("1000");
        dto.setCellphone("3001112233");
        dto.setGender(Gender.HOMBRE);
        return dto;
    }

    private User operatorA() {
        return TestFixtures.user("user-op", "operador-a",
                TestFixtures.member("op-member", branchA, "Operador A"));
    }

    private User administratorA() {
        return TestFixtures.user("user-admin", "admin-a",
                TestFixtures.member("admin-member", branchA, "Admin A"),
                AppPermission.ADMINISTRATOR.name());
    }

    private void answerMapperWithRequestName() {
        doAnswer(invocation -> {
            MemberRequestDto dto = invocation.getArgument(0);
            Member member = invocation.getArgument(1);
            member.setCompleteName(dto.getCompleteName());
            return null;
        }).when(memberMapper).updateEntityFromDto(any(MemberRequestDto.class), any(Member.class));
    }

    @Test
    void updateFromAnotherBranchIsRejectedBeforeMapper_H22() {
        Member foreignMember = memberIn(branchB, "Ana Perez B");
        when(memberRepository.findById(foreignMember.getId())).thenReturn(Optional.of(foreignMember));
        // Si la comprobación de sede se ejecutara después del mapper, este lanzaría la
        // excepción que filtra el nombre completo del integrante de la otra sede.
        lenient().doThrow(new MemberNoHasCategoryForThisSubCategoryException(foreignMember.getCompleteName()))
                .when(memberMapper).updateEntityFromDto(any(MemberRequestDto.class), any(Member.class));

        assertThatThrownBy(() -> memberPersistenceService.update(
                foreignMember.getId(), request("Nombre Hackeado", "sub-incoherente"), null, operatorA()))
                .isInstanceOfSatisfying(CannotUpdateMemberOutsideYourBranchException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .isNotInstanceOf(MemberNoHasCategoryForThisSubCategoryException.class);

        verify(memberMapper, never()).updateEntityFromDto(any(MemberRequestDto.class), any(Member.class));
        verify(memberRepository, never()).save(any(Member.class));
        verifyNoInteractions(branchRepository, memberSubCategoryRepository);
        assertThat(foreignMember.getCompleteName()).isEqualTo("Ana Perez B");
        assertThat(foreignMember.getCc()).isEqualTo("1000");
        assertThat(foreignMember.getBranch()).isSameAs(branchB);
        assertThat(foreignMember.getUpdatedAt()).isNull();
    }

    @Test
    void updateFromSameBranchReachesTheMapper_H22() {
        Member ownMember = memberIn(branchA, "Ana Perez");
        MemberRequestDto dto = request("Ana Actualizada", null);
        when(memberRepository.findById(ownMember.getId())).thenReturn(Optional.of(ownMember));
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        answerMapperWithRequestName();
        when(memberRepository.save(ownMember)).thenReturn(ownMember);

        memberPersistenceService.update(ownMember.getId(), dto, null, operatorA());

        verify(memberMapper).updateEntityFromDto(dto, ownMember);
        verify(memberRepository).save(ownMember);
        assertThat(ownMember.getCompleteName()).isEqualTo("Ana Actualizada");
        assertThat(ownMember.getUpdatedAt()).isEqualTo(TestFixtures.FIXED_NOW);
        assertThat(ownMember.getBranch()).isSameAs(branchA);
    }

    @Test
    void administratorCanUpdateMemberFromAnotherBranch_H22() {
        Member foreignMember = memberIn(branchB, "Beto B");
        MemberRequestDto dto = request("Beto Actualizado", null);
        dto.setBranchId(branchA.getId());
        when(memberRepository.findById(foreignMember.getId())).thenReturn(Optional.of(foreignMember));
        when(branchRepository.findById(branchA.getId())).thenReturn(Optional.of(branchA));
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        answerMapperWithRequestName();
        when(memberRepository.save(foreignMember)).thenReturn(foreignMember);

        memberPersistenceService.update(foreignMember.getId(), dto, null, administratorA());

        verify(memberMapper).updateEntityFromDto(dto, foreignMember);
        verify(branchRepository).findById(branchA.getId());
        verify(memberRepository).save(foreignMember);
        assertThat(foreignMember.getCompleteName()).isEqualTo("Beto Actualizado");
        assertThat(foreignMember.getBranch()).isSameAs(branchA);
    }
}
