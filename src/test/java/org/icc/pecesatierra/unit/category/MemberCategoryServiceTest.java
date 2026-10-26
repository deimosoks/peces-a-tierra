package org.icc.pecesatierra.unit.category;

import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.category.dtos.MemberCategoryRequestDto;
import org.icc.pecesatierra.features.category.dtos.MemberCategoryResponseDto;
import org.icc.pecesatierra.features.category.exceptions.CategoryNotFoundException;
import org.icc.pecesatierra.features.category.exceptions.CategoryWithNameAlreadyExistsException;
import org.icc.pecesatierra.features.category.mapper.MemberCategoryMapper;
import org.icc.pecesatierra.features.category.repository.MemberCategoryRepository;
import org.icc.pecesatierra.features.category.service.MemberCategoryService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberCategoryServiceTest {

    @Mock
    private MemberCategoryRepository memberCategoryRepository;
    @Mock
    private MemberCategoryMapper memberCategoryMapper;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberCategoryService memberCategoryService;

    private final User user = TestFixtures.user("user-1", "operador-a",
            TestFixtures.member("op-member", TestFixtures.branch("branch-a", "Sede A"), "Operador A"));

    private MemberCategory category(String id, String name) {
        return MemberCategory.builder()
                .id(id)
                .name(name)
                .color("#112233")
                .build();
    }

    private MemberCategoryRequestDto request(String name) {
        return MemberCategoryRequestDto.builder()
                .name(name)
                .color("#445566")
                .build();
    }

    private void answerMapperWithRequestName() {
        doAnswer(invocation -> {
            MemberCategoryRequestDto dto = invocation.getArgument(0);
            MemberCategory target = invocation.getArgument(1);
            target.setName(dto.getName());
            target.setColor(dto.getColor());
            return null;
        }).when(memberCategoryMapper).updateEntityFromDto(any(MemberCategoryRequestDto.class), any(MemberCategory.class));
    }

    @Test
    void updateRenamingToANameUsedByAnotherCategoryConflicts_H42b() {
        MemberCategory existing = category("cat-1", "Jovenes");
        when(memberCategoryRepository.findById("cat-1")).thenReturn(Optional.of(existing));
        when(memberCategoryRepository.existsByNameAndIdNot("Ninos", "cat-1")).thenReturn(true);

        assertThatThrownBy(() -> memberCategoryService.update(request("Ninos"), "cat-1", user))
                .isInstanceOfSatisfying(CategoryWithNameAlreadyExistsException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).contains("Ninos");
                });

        // No se toca la entidad ni se mapea nada: el 409 corta antes de guardar.
        verify(memberCategoryMapper, never()).updateEntityFromDto(any(MemberCategoryRequestDto.class), any(MemberCategory.class));
        verifyNoInteractions(memberRepository);
        assertThat(existing.getName()).isEqualTo("Jovenes");
        assertThat(existing.getColor()).isEqualTo("#112233");
    }

    @Test
    void updateKeepingTheSameNameDoesNotConflictWithItself_H42b() {
        MemberCategory existing = category("cat-1", "Jovenes");
        when(memberCategoryRepository.findById("cat-1")).thenReturn(Optional.of(existing));
        when(memberCategoryRepository.existsByNameAndIdNot("Jovenes", "cat-1")).thenReturn(false);
        when(memberCategoryMapper.toDto(existing)).thenAnswer(invocation -> {
            MemberCategory saved = invocation.getArgument(0);
            return MemberCategoryResponseDto.builder()
                    .id(saved.getId())
                    .name(saved.getName())
                    .color(saved.getColor())
                    .build();
        });
        answerMapperWithRequestName();

        MemberCategoryResponseDto response = memberCategoryService.update(request("Jovenes"), "cat-1", user);

        // La comprobacion excluye la propia categoria: no consulta la variante
        // sin id, que devolveria true y provocaria un 409 al guardar sin cambios.
        verify(memberCategoryRepository).existsByNameAndIdNot("Jovenes", "cat-1");
        verify(memberCategoryRepository, never()).existsByName(anyString());
        assertThat(response.getId()).isEqualTo("cat-1");
        assertThat(response.getName()).isEqualTo("Jovenes");
        assertThat(existing.getColor()).isEqualTo("#445566");
    }

    @Test
    void updateOfAnUnknownCategoryIsNotFoundBeforeCheckingUniqueness_H42b() {
        when(memberCategoryRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberCategoryService.update(request("Adultos"), "missing", user))
                .isInstanceOfSatisfying(CategoryNotFoundException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        // El 404 va delante: sobre una categoria inexistente no se consulta unicidad.
        verify(memberCategoryRepository, never()).existsByNameAndIdNot(anyString(), anyString());
        verify(memberCategoryRepository, never()).existsByName(anyString());
        verifyNoInteractions(memberCategoryMapper, memberRepository);
    }
}
