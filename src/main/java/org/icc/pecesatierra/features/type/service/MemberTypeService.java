package org.icc.pecesatierra.features.type.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.features.type.dtos.MemberTypeRequestDto;
import org.icc.pecesatierra.features.type.dtos.MemberTypeResponseDto;
import org.icc.pecesatierra.features.type.MemberType;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.member.exceptions.CannotDeleteMemberTypeInUseException;
import org.icc.pecesatierra.features.type.exceptions.TypeWithNameAlreadyExistsException;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.type.exceptions.TypeNotFoundException;
import org.icc.pecesatierra.features.type.repository.MemberTypeRepository;
import org.icc.pecesatierra.features.category.repository.mapper.MemberTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberTypeService {

    private final MemberTypeRepository memberTypeRepository;
    private final MemberTypeMapper memberTypeMapper;
    private final MemberRepository memberRepository;

    @Transactional
    public MemberTypeResponseDto create(MemberTypeRequestDto memberTypeRequestDto, User user) {

        if (memberTypeRepository.existsByName(memberTypeRequestDto.getName())) {
            log.warn("Usuario {} intento crear tipo con nombre '{}' pero ya existe.", user.getMember().getId(), memberTypeRequestDto.getName());
            throw new TypeWithNameAlreadyExistsException(memberTypeRequestDto.getName());
        }

        MemberType memberType = MemberType.builder()
                .name(memberTypeRequestDto.getName())
                .color(memberTypeRequestDto.getColor())
                .build();

        memberTypeRepository.save(memberType);

        log.info("""
                        Usuario {} creó un tipo de integrante:
                        ID: {}
                        Nombre: {}
                        Color: {}
                        """,
                user.getMember().getId(),
                memberType.getId(),
                memberType.getName(),
                memberType.getColor());

        return memberTypeMapper.toDto(memberType);
    }

    @Transactional
    public MemberTypeResponseDto update(MemberTypeRequestDto memberCategoryRequestDto, String typeId) {
        MemberType memberType = memberTypeRepository.findById(typeId)
                .orElseThrow(TypeNotFoundException::new);

        MemberType beforeUpdate = MemberType.builder()
                .id(memberType.getId())
                .name(memberType.getName())
                .color(memberType.getColor())
                .build();

        memberTypeMapper.updateEntityFromDto(memberCategoryRequestDto, memberType);

        log.info("""
                        Usuario actualizó el tipo de integrante {}.
                        Estado anterior:
                        Nombre: {}
                        Color: {}
                        Nuevo estado:
                        Nombre: {}
                        Color: {}
                        """,
                memberType.getId(),
                beforeUpdate.getName(),
                beforeUpdate.getColor(),
                memberType.getName(),
                memberType.getColor());

        return memberTypeMapper.toDto(memberType);
    }

    @Transactional
    public void delete(String typeId, User user) {
        MemberType memberType = memberTypeRepository.findById(typeId)
                .orElseThrow(TypeNotFoundException::new);

        if (memberRepository.existsMemberByTypeId(memberType)) {
            log.warn("Usuario {} intento eliminar tipo '{}' pero está en uso.", user.getMember().getId(), memberType.getName());
            throw new CannotDeleteMemberTypeInUseException(memberType.getName());
        }

        log.info("""
                        Usuario {} eliminó el tipo de integrante:
                        ID: {}
                        Nombre: {}
                        Color: {}
                        """,
                user.getMember().getId(),
                memberType.getId(),
                memberType.getName(),
                memberType.getColor());

        memberTypeRepository.delete(memberType);
    }

    @Transactional(readOnly = true)
    public List<MemberTypeResponseDto> findAll() {
        return memberTypeRepository.findAll().stream().map(memberTypeMapper::toDto).toList();
    }
}
