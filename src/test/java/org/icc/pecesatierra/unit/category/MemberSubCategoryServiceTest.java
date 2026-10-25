package org.icc.pecesatierra.unit.category;

import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.category.MemberSubCategory;
import org.icc.pecesatierra.features.category.dtos.MemberSubCategoryRequestDto;
import org.icc.pecesatierra.features.category.dtos.MemberSubCategoryResponseDto;
import org.icc.pecesatierra.features.category.exceptions.sub.AlreadyExistsSubCategoryWithName;
import org.icc.pecesatierra.features.category.mapper.MemberSubCategoryMapper;
import org.icc.pecesatierra.features.category.repository.MemberCategoryRepository;
import org.icc.pecesatierra.features.category.repository.MemberSubCategoryRepository;
import org.icc.pecesatierra.features.category.service.MemberSubCategoryService;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberSubCategoryServiceTest {

    @Mock
    private MemberSubCategoryRepository memberSubCategoryRepository;
    @Mock
    private MemberCategoryRepository memberCategoryRepository;
    @Mock
    private MemberSubCategoryMapper memberSubCategoryMapper;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberSubCategoryService memberSubCategoryService;

    private final User user = TestFixtures.user("user-1", "operador-a",
            TestFixtures.member("op-member", TestFixtures.branch("branch-a", "Sede A"), "Operador A"));
    private final MemberCategory category = MemberCategory.builder()
            .id("cat-1")
            .name("Categoria")
            .color("#112233")
            .build();

    private MemberSubCategory subCategory(String id, String name) {
        return MemberSubCategory.builder()
                .id(id)
                .name(name)
                .color("#112233")
                .category(category)
                .build();
    }

    private MemberSubCategoryRequestDto request(String name) {
        return MemberSubCategoryRequestDto.builder()
                .name(name)
                .color("#112233")
                .categoryId(category.getId())
                .build();
    }

    private void answerMapperWithRequestName() {
        doAnswer(invocation -> {
            MemberSubCategoryRequestDto dto = invocation.getArgument(0);
            MemberSubCategory target = invocation.getArgument(1);
            target.setName(dto.getName());
            target.setColor(dto.getColor());
            return null;
        }).when(memberSubCategoryMapper).updateEntityFromDto(any(MemberSubCategoryRequestDto.class), any(MemberSubCategory.class));
    }

    @Test
    void updateRenamingToAFreeNameIsSaved_H25() {
        MemberSubCategory existing = subCategory("sub-1", "Jovenes");
        when(memberSubCategoryRepository.findById("sub-1")).thenReturn(Optional.of(existing));
        when(memberSubCategoryRepository.existsByCategoryAndNameAndIdNot(category, "Adultos", "sub-1")).thenReturn(false);
        when(memberSubCategoryMapper.toDto(existing)).thenAnswer(invocation -> {
            MemberSubCategory saved = invocation.getArgument(0);
            return MemberSubCategoryResponseDto.builder()
                    .id(saved.getId())
                    .name(saved.getName())
                    .color(saved.getColor())
                    .build();
        });
        answerMapperWithRequestName();

        MemberSubCategoryResponseDto response = memberSubCategoryService.update(request("Adultos"), "sub-1", user);

        verify(memberSubCategoryRepository).existsByCategoryAndNameAndIdNot(category, "Adultos", "sub-1");
        verify(memberSubCategoryMapper).updateEntityFromDto(any(MemberSubCategoryRequestDto.class), any(MemberSubCategory.class));
        assertThat(response.getId()).isEqualTo("sub-1");
        assertThat(response.getName()).isEqualTo("Adultos");
        assertThat(existing.getName()).isEqualTo("Adultos");
    }

    @Test
    void updateKeepingTheSameNameDoesNotConflictWithItself_H25() {
        MemberSubCategory existing = subCategory("sub-1", "Jovenes");
        when(memberSubCategoryRepository.findById("sub-1")).thenReturn(Optional.of(existing));
        when(memberSubCategoryRepository.existsByCategoryAndNameAndIdNot(category, "Jovenes", "sub-1")).thenReturn(false);
        // La comprobacion anterior incluia la propia sub-categoria: devolvia true y
        // provocaba un 409 al guardar sin tocar el nombre.
        lenient().when(memberSubCategoryRepository.existsByCategoryAndName(category, "Jovenes")).thenReturn(true);
        when(memberSubCategoryMapper.toDto(existing)).thenAnswer(invocation -> {
            MemberSubCategory saved = invocation.getArgument(0);
            return MemberSubCategoryResponseDto.builder()
                    .id(saved.getId())
                    .name(saved.getName())
                    .color(saved.getColor())
                    .build();
        });
        answerMapperWithRequestName();

        MemberSubCategoryResponseDto response = memberSubCategoryService.update(request("Jovenes"), "sub-1", user);

        verify(memberSubCategoryRepository).existsByCategoryAndNameAndIdNot(category, "Jovenes", "sub-1");
        verify(memberSubCategoryRepository, never()).existsByCategoryAndName(any(MemberCategory.class), anyString());
        assertThat(response.getName()).isEqualTo("Jovenes");
    }

    @Test
    void updateRenamingToANameUsedByAnotherSubCategoryConflicts_H25() {
        MemberSubCategory existing = subCategory("sub-1", "Jovenes");
        when(memberSubCategoryRepository.findById("sub-1")).thenReturn(Optional.of(existing));
        when(memberSubCategoryRepository.existsByCategoryAndNameAndIdNot(category, "Ninos", "sub-1")).thenReturn(true);

        assertThatThrownBy(() -> memberSubCategoryService.update(request("Ninos"), "sub-1", user))
                .isInstanceOfSatisfying(AlreadyExistsSubCategoryWithName.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(memberSubCategoryMapper, never()).updateEntityFromDto(any(MemberSubCategoryRequestDto.class), any(MemberSubCategory.class));
        verifyNoInteractions(memberCategoryRepository, memberRepository);
        assertThat(existing.getName()).isEqualTo("Jovenes");
    }
}
