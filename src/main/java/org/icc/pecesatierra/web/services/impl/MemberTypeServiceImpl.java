package org.icc.pecesatierra.web.services.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.member.type.MemberTypeRequestDto;
import org.icc.pecesatierra.dtos.member.type.MemberTypeResponseDto;
import org.icc.pecesatierra.entities.MemberType;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.members.CannotDeleteMemberTypeInUseException;
import org.icc.pecesatierra.exceptions.members.types.TypeWithNameAlreadyExistsException;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.exceptions.members.types.TypeNotFoundException;
import org.icc.pecesatierra.repositories.MemberTypeRepository;
import org.icc.pecesatierra.utils.mappers.MemberTypeMapper;
import org.icc.pecesatierra.web.services.MemberTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberTypeServiceImpl implements MemberTypeService {

    private final MemberTypeRepository memberTypeRepository;
    private final MemberTypeMapper memberTypeMapper;
    private final MemberRepository memberRepository;

    @Transactional
    @Override
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
    @Override
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
    @Override
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
    @Override
    public List<MemberTypeResponseDto> findAll() {
        return memberTypeRepository.findAll().stream().map(memberTypeMapper::toDto).toList();
    }
}
