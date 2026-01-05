package org.icc.pecesatierra.services;

import org.icc.pecesatierra.dtos.role.RoleRequestDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;

import java.util.List;

public interface RoleService {
    RoleResponseDto create(RoleRequestDto roleRequestDto);

    RoleResponseDto update(RoleRequestDto roleRequestDto, String rolId);

    void delete(String roleId);

    List<RoleResponseDto> findAll();
}
