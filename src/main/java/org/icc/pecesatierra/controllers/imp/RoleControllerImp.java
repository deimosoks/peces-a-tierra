package org.icc.pecesatierra.controllers.imp;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.controllers.RoleController;
import org.icc.pecesatierra.dtos.role.RoleRequestDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;
import org.icc.pecesatierra.services.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/roles")
@AllArgsConstructor
@RestController
public class RoleControllerImp implements RoleController {

    private final RoleService roleService;

    @Override
    @PostMapping
    public ResponseEntity<RoleResponseDto> create(@Valid @RequestBody RoleRequestDto roleRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(roleRequestDto));
    }

    @Override
    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponseDto> update(@Valid @RequestBody RoleRequestDto roleRequestDto,
                                                  @PathVariable String roleId) {
        return ResponseEntity.ok(roleService.update(roleRequestDto, roleId));
    }

    @Override
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> delete(@PathVariable String roleId) {
        roleService.delete(roleId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping
    public ResponseEntity<List<RoleResponseDto>> findAll() {
        return ResponseEntity.ok(roleService.findAll());
    }
}
