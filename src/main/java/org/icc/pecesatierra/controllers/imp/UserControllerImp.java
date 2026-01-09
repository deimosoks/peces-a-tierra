package org.icc.pecesatierra.controllers.imp;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.controllers.UserController;
import org.icc.pecesatierra.dtos.user.*;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserControllerImp implements UserController {

    private final UserService userService;

    @Override
    @PreAuthorize("hasAuthority('VIEW_USER_PANEL')")
    @PostMapping("/report")
    public ResponseEntity<UserReportResponseDto> report() {
        return ResponseEntity.ok(userService.report());
    }

    @Override
    @PreAuthorize("hasAuthority('CREATE_USER')")
    @PostMapping
    public ResponseEntity<UserResponseDto> create(@RequestBody @Valid UserRequestDto userRequestDto,
                                                  @AuthenticationPrincipal User givenBy) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(userRequestDto, givenBy));
    }

    @Override
    @GetMapping("/@me")
    public ResponseEntity<MeDto> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.me(user));
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_USER_PANEL')")
    @GetMapping
    public ResponseEntity<UserPagesResponseDto> findAll(@RequestParam int page) {
        return ResponseEntity.ok(userService.findAll(page));
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_USER_PANEL')")
    @GetMapping("/query")
    public ResponseEntity<UserPagesResponseDto> findAllByQuery(@RequestParam int page,
                                                               @RequestParam String query) {
        return ResponseEntity.ok(userService.findByQuery(query,page));
    }

    @Override
    @PreAuthorize("hasAuthority('UPDATE_USER')")
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseDto> update(@RequestBody UserRequestDto userRequestDto,
                                                  @PathVariable String userId,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.update(userRequestDto, userId, user));
    }

    @Override
    @PreAuthorize("hasAuthority('UPDATE_USER')")
    @PatchMapping("/{userId}")
    public ResponseEntity<Boolean> updateActive(@PathVariable String userId,
                                             @RequestParam boolean active) {
        return ResponseEntity.ok(userService.updateActive(userId, active));
    }

    @Override
    @PreAuthorize("hasAuthority('DELETE_USER')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable String userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }

}
