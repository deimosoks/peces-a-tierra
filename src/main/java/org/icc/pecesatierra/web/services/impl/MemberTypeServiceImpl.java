package org.icc.pecesatierra.web.services.impl;


import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.member.MemberTypeRequestDto;
import org.icc.pecesatierra.dtos.member.MemberTypeResponseDto;
import org.icc.pecesatierra.entities.MemberType;
import org.icc.pecesatierra.exceptions.MemberTypeInUseException;
import org.icc.pecesatierra.exceptions.TypeWithNameAlreadyExistsException;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.exceptions.MemberTypeNotFoundException;
import org.icc.pecesatierra.repositories.MemberTypeRepository;
import org.icc.pecesatierra.utils.mappers.MemberTypeMapper;
import org.icc.pecesatierra.web.services.MemberTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberTypeServiceImpl implements MemberTypeService {

    private final MemberTypeRepository memberTypeRepository;
    private final MemberTypeMapper memberTypeMapper;
    private final MemberRepository memberRepository;

    @Transactional
    @Override
    public MemberTypeResponseDto create(MemberTypeRequestDto memberTypeRequestDto) {

        if (memberTypeRepository.existsByName(memberTypeRequestDto.getName())){
            throw new TypeWithNameAlreadyExistsException("Ya existe un tipo con el nombre " + memberTypeRequestDto.getName());
        }

        MemberType memberType = MemberType.builder()
                .name(memberTypeRequestDto.getName())
                .color(memberTypeRequestDto.getColor())
                .build();

        return memberTypeMapper.toDto(memberTypeRepository.save(memberType));
    }

    @Transactional
    @Override
    public MemberTypeResponseDto update(MemberTypeRequestDto memberCategoryRequestDto, String string) {
        MemberType memberType = memberTypeRepository.findById(string)
                .orElseThrow(()-> new MemberTypeNotFoundException("Tipo no encontrado."));
        memberTypeMapper.updateEntityFromDto(memberCategoryRequestDto, memberType);
        return memberTypeMapper.toDto(memberTypeRepository.save(memberType));
    }

    @Transactional
    @Override
    public void delete(String typeId) {
        MemberType memberType = memberTypeRepository.findById(typeId)
                .orElseThrow(()-> new MemberTypeNotFoundException("Tipo no encontrado."));

        if (memberRepository.existsMemberByTypeId(memberType)){
            throw new MemberTypeInUseException("Este tipo esta en uso asi que no puede ser eliminado.");
        }

        memberTypeRepository.delete(memberType);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MemberTypeResponseDto> findAll() {
        return memberTypeRepository.findAll().stream().map(memberTypeMapper::toDto).toList();
    }
}
