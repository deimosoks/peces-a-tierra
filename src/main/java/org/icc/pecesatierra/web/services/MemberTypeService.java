package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.member.type.MemberTypeRequestDto;
import org.icc.pecesatierra.dtos.member.type.MemberTypeResponseDto;
import org.icc.pecesatierra.entities.User;

import java.util.List;

public interface MemberTypeService {

    MemberTypeResponseDto create(MemberTypeRequestDto memberTypeRequestDto, User user);

    MemberTypeResponseDto update(MemberTypeRequestDto memberCategoryRequestDto, String memberCategoryId);

    void delete(String typeId, User user);

    List<MemberTypeResponseDto> findAll();

}
