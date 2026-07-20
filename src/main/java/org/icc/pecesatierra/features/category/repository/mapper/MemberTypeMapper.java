package org.icc.pecesatierra.features.category.repository.mapper;

import org.icc.pecesatierra.features.type.dtos.MemberTypeRequestDto;
import org.icc.pecesatierra.features.type.dtos.MemberTypeResponseDto;
import org.icc.pecesatierra.features.type.MemberType;
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
