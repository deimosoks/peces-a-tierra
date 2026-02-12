package org.icc.pecesatierra.utils.mappers;

import org.icc.pecesatierra.dtos.member.MemberCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.MemberCategoryResponseDto;
import org.icc.pecesatierra.entities.MemberCategory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface MemberCategoryMapper {

    MemberCategoryResponseDto toDto(MemberCategory memberCategory);

    void updateEntityFromDto(MemberCategoryRequestDto memberCategoryRequestDto, @MappingTarget MemberCategory memberCategory);

}
