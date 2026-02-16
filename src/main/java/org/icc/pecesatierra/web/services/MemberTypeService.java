package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.member.type.MemberTypeRequestDto;
import org.icc.pecesatierra.dtos.member.type.MemberTypeResponseDto;

import java.util.List;

public interface MemberTypeService {

    MemberTypeResponseDto create(MemberTypeRequestDto memberTypeRequestDto);

    MemberTypeResponseDto update(MemberTypeRequestDto memberCategoryRequestDto, String memberCategoryId);

    void delete(String typeId);

    List<MemberTypeResponseDto> findAll();

}
