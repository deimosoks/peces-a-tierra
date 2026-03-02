package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.role.RoleRequestDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;
import org.icc.pecesatierra.entities.User;

import java.util.List;

public interface RoleService {
    RoleResponseDto create(RoleRequestDto roleRequestDto, User user);

    RoleResponseDto update(RoleRequestDto roleRequestDto, String rolId, User user);

    void delete(String roleId, User user);

    List<RoleResponseDto> findAll();
}
