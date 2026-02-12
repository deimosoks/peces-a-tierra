package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.member.MemberCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.MemberCategoryResponseDto;
import org.icc.pecesatierra.dtos.member.MemberTypeRequestDto;
import org.icc.pecesatierra.dtos.member.MemberTypeResponseDto;

import java.util.List;

public interface MemberTypeService {

    MemberTypeResponseDto create(MemberTypeRequestDto memberTypeRequestDto);

    MemberTypeResponseDto update(MemberTypeRequestDto memberCategoryRequestDto, String memberCategoryId);

    void delete(String typeId);

    List<MemberTypeResponseDto> findAll();

}
