package org.icc.pecesatierra.helpers.mappers;

import org.icc.pecesatierra.dtos.permission.PermissionResponseDto;
import org.icc.pecesatierra.domain.reference.Permission;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface PermissionMapper {
    PermissionResponseDto toDto(Permission permission);
}
