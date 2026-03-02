package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.member.category.MemberCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.category.MemberCategoryResponseDto;
import org.icc.pecesatierra.entities.User;

import java.util.List;

public interface MemberCategoryService {

    MemberCategoryResponseDto create(MemberCategoryRequestDto memberCategoryRequestDto, User user);

    MemberCategoryResponseDto update(MemberCategoryRequestDto memberCategoryRequestDto, String categoryId, User user);

    void delete(String categoryId, User user);

    List<MemberCategoryResponseDto> findAll();
}
