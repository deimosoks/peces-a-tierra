package org.icc.pecesatierra.utils.mappers;

import org.icc.pecesatierra.dtos.member.category.MemberSubCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.category.MemberSubCategoryResponseDto;
import org.icc.pecesatierra.entities.MemberSubCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface MemberSubCategoryMapper {

    MemberSubCategoryResponseDto toDto(MemberSubCategory memberSubCategory);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateEntityFromDto(MemberSubCategoryRequestDto memberSubCategoryRequestDto,@MappingTarget MemberSubCategory memberSubCategory);

}
