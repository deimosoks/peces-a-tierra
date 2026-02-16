package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.member.category.MemberCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.category.MemberCategoryResponseDto;

import java.util.List;

public interface MemberCategoryService {

    MemberCategoryResponseDto create(MemberCategoryRequestDto memberCategoryRequestDto);

    MemberCategoryResponseDto update(MemberCategoryRequestDto memberCategoryRequestDto, String categoryId);

    void delete(String categoryId);

    List<MemberCategoryResponseDto> findAll();
}
