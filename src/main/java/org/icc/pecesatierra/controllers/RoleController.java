package org.icc.pecesatierra.controllers;

import org.icc.pecesatierra.dtos.role.RoleRequestDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface RoleController {
    ResponseEntity<RoleResponseDto> create(RoleRequestDto roleRequestDto);
    ResponseEntity<RoleResponseDto> update(RoleRequestDto roleRequestDto, String roleId);
    ResponseEntity<Void> delete(String roleId);
    ResponseEntity<List<RoleResponseDto>> findAll();
}
