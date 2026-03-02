package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.member.category.MemberSubCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.category.MemberSubCategoryResponseDto;
import org.icc.pecesatierra.entities.User;

public interface MemberSubCategoryService {

    MemberSubCategoryResponseDto create(MemberSubCategoryRequestDto memberSubCategoryRequestDto, User user);

    MemberSubCategoryResponseDto update(MemberSubCategoryRequestDto memberSubCategoryRequestDto, String subCategoryId, User user);

    void delete(String subCategoryId, User user);

}
