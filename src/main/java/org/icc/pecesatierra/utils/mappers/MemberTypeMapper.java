package org.icc.pecesatierra.utils.mappers;

import org.icc.pecesatierra.dtos.member.MemberTypeRequestDto;
import org.icc.pecesatierra.dtos.member.MemberTypeResponseDto;
import org.icc.pecesatierra.entities.MemberType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

import java.lang.annotation.Target;

@Mapper(componentModel = "spring")
@Component
public interface MemberTypeMapper {

    MemberTypeResponseDto toDto(MemberType memberType);

    void updateEntityFromDto(MemberTypeRequestDto memberTypeRequestDto, @MappingTarget MemberType memberType);
}
