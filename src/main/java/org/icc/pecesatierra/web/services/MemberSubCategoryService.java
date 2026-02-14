package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.member.MemberSubCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.MemberSubCategoryResponseDto;

public interface MemberSubCategoryService {

    MemberSubCategoryResponseDto create(MemberSubCategoryRequestDto memberSubCategoryRequestDto);

    MemberSubCategoryResponseDto update(MemberSubCategoryRequestDto memberSubCategoryRequestDto, String subCategoryId);

    void delete(String subCategoryId);
//    List<>

}
