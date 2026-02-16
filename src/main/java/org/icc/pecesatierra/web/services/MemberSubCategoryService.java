package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.member.category.MemberSubCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.category.MemberSubCategoryResponseDto;

public interface MemberSubCategoryService {

    MemberSubCategoryResponseDto create(MemberSubCategoryRequestDto memberSubCategoryRequestDto);

    MemberSubCategoryResponseDto update(MemberSubCategoryRequestDto memberSubCategoryRequestDto, String subCategoryId);

    void delete(String subCategoryId);
//    List<>

}
