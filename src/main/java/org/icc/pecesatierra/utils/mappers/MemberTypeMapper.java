package org.icc.pecesatierra.utils.mappers;

import org.icc.pecesatierra.dtos.member.type.MemberTypeRequestDto;
import org.icc.pecesatierra.dtos.member.type.MemberTypeResponseDto;
import org.icc.pecesatierra.entities.MemberType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface MemberTypeMapper {

    MemberTypeResponseDto toDto(MemberType memberType);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(MemberTypeRequestDto memberTypeRequestDto, @MappingTarget MemberType memberType);
}
