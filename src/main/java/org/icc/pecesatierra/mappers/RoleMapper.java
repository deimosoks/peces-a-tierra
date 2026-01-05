package org.icc.pecesatierra.mappers;

import org.icc.pecesatierra.dtos.role.RoleRequestDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;
import org.icc.pecesatierra.entities.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface RoleMapper {
    RoleResponseDto toDto(Role role);

    @Mapping(target = "permissions", ignore = true)
    void updateEntityFromDto(RoleRequestDto roleRequestDto, @MappingTarget Role role);
}
