package org.icc.pecesatierra.utils.mappers;

import org.icc.pecesatierra.dtos.member.MemberSubCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.MemberSubCategoryResponseDto;
import org.icc.pecesatierra.entities.MemberSubCategory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface MemberSubCategoryMapper {

    MemberSubCategoryResponseDto toDto(MemberSubCategory memberSubCategory);

    void updateEntityFromDto(MemberSubCategoryRequestDto memberSubCategoryRequestDto,@MappingTarget MemberSubCategory memberSubCategory);

}
