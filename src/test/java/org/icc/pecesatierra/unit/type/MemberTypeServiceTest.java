package org.icc.pecesatierra.unit.type;

import org.icc.pecesatierra.features.category.repository.mapper.MemberTypeMapper;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.type.MemberType;
import org.icc.pecesatierra.features.type.dtos.MemberTypeRequestDto;
import org.icc.pecesatierra.features.type.dtos.MemberTypeResponseDto;
import org.icc.pecesatierra.features.type.exceptions.TypeWithNameAlreadyExistsException;
import org.icc.pecesatierra.features.type.repository.MemberTypeRepository;
import org.icc.pecesatierra.features.type.service.MemberTypeService;
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
class MemberTypeServiceTest {

    @Mock
    private MemberTypeRepository memberTypeRepository;
    @Mock
    private MemberTypeMapper memberTypeMapper;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberTypeService memberTypeService;

    private MemberType type(String id, String name) {
        return MemberType.builder()
                .id(id)
                .name(name)
                .color("#112233")
                .build();
    }

    private MemberTypeRequestDto request(String name) {
        return MemberTypeRequestDto.builder()
                .name(name)
                .color("#112233")
                .build();
    }

    private void answerMapperWithRequestName() {
        doAnswer(invocation -> {
            MemberTypeRequestDto dto = invocation.getArgument(0);
            MemberType target = invocation.getArgument(1);
            target.setName(dto.getName());
            target.setColor(dto.getColor());
            return null;
        }).when(memberTypeMapper).updateEntityFromDto(any(MemberTypeRequestDto.class), any(MemberType.class));
    }

    @Test
    void updateRenamingToAFreeNameIsSaved_H42A() {
        MemberType existing = type("type-1", "Jovenes");
        when(memberTypeRepository.findById("type-1")).thenReturn(Optional.of(existing));
        when(memberTypeRepository.existsByNameAndIdNot("Adultos", "type-1")).thenReturn(false);
        when(memberTypeMapper.toDto(existing)).thenAnswer(invocation -> {
            MemberType saved = invocation.getArgument(0);
            return MemberTypeResponseDto.builder()
                    .id(saved.getId())
                    .name(saved.getName())
                    .color(saved.getColor())
                    .build();
        });
        answerMapperWithRequestName();

        MemberTypeResponseDto response = memberTypeService.update(request("Adultos"), "type-1");

        verify(memberTypeRepository).existsByNameAndIdNot("Adultos", "type-1");
        verify(memberTypeMapper).updateEntityFromDto(any(MemberTypeRequestDto.class), any(MemberType.class));
        assertThat(response.getId()).isEqualTo("type-1");
        assertThat(response.getName()).isEqualTo("Adultos");
        assertThat(existing.getName()).isEqualTo("Adultos");
    }

    @Test
    void updateKeepingTheSameNameDoesNotConflictWithItself_H42A() {
        MemberType existing = type("type-1", "Jovenes");
        when(memberTypeRepository.findById("type-1")).thenReturn(Optional.of(existing));
        when(memberTypeRepository.existsByNameAndIdNot("Jovenes", "type-1")).thenReturn(false);
        // Comprobar con existsByName incluiria el propio tipo y daria un 409 falso.
        lenient().when(memberTypeRepository.existsByName("Jovenes")).thenReturn(true);
        when(memberTypeMapper.toDto(existing)).thenAnswer(invocation -> {
            MemberType saved = invocation.getArgument(0);
            return MemberTypeResponseDto.builder()
                    .id(saved.getId())
                    .name(saved.getName())
                    .color(saved.getColor())
                    .build();
        });
        answerMapperWithRequestName();

        MemberTypeResponseDto response = memberTypeService.update(request("Jovenes"), "type-1");

        verify(memberTypeRepository).existsByNameAndIdNot("Jovenes", "type-1");
        verify(memberTypeRepository, never()).existsByName(anyString());
        assertThat(response.getName()).isEqualTo("Jovenes");
    }

    @Test
    void updateRenamingToANameUsedByAnotherTypeConflicts_H42A() {
        MemberType existing = type("type-1", "Jovenes");
        when(memberTypeRepository.findById("type-1")).thenReturn(Optional.of(existing));
        when(memberTypeRepository.existsByNameAndIdNot("Ninos", "type-1")).thenReturn(true);

        assertThatThrownBy(() -> memberTypeService.update(request("Ninos"), "type-1"))
                .isInstanceOfSatisfying(TypeWithNameAlreadyExistsException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(memberTypeMapper, never()).updateEntityFromDto(any(MemberTypeRequestDto.class), any(MemberType.class));
        verifyNoInteractions(memberRepository);
        assertThat(existing.getName()).isEqualTo("Jovenes");
    }
}
