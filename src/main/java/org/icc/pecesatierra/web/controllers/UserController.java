package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.domain.reference.User;
import org.icc.pecesatierra.dtos.user.*;
import org.icc.pecesatierra.web.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController extends BaseController  {

    private final UserService userService;

    @PreAuthorize("hasAuthority('VIEW_USER_PANEL')")
    @PostMapping("/report")
    public ResponseEntity<UserReportResponseDto> report() {
        return ResponseEntity.ok(userService.report());
    }

    @PreAuthorize("hasAuthority('CREATE_USER')")
    @PostMapping
    public ResponseEntity<UserResponseDto> create(@RequestBody @Valid UserRequestDto userRequestDto,
                                                  @AuthenticationPrincipal User givenBy) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(userRequestDto, givenBy));
    }

    @GetMapping("/@me")
    public ResponseEntity<MeDto> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.me(user));
    }

    @PreAuthorize("hasAuthority('VIEW_USER_PANEL')")
    @GetMapping
    public ResponseEntity<UserPagesResponseDto> findAll(@RequestParam int page) {
        return ResponseEntity.ok(userService.findAll(page));
    }

    @PreAuthorize("hasAuthority('VIEW_USER_PANEL')")
    @GetMapping("/query")
    public ResponseEntity<UserPagesResponseDto> findAllByQuery(@RequestParam int page,
                                                               @RequestParam String query) {
        return ResponseEntity.ok(userService.findByQuery(query, page));
    }

    @PreAuthorize("hasAuthority('UPDATE_USER')")
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseDto> update(@RequestBody UserRequestDto userRequestDto,
                                                  @PathVariable String userId,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.update(userRequestDto, userId, user));
    }

    @PreAuthorize("hasAuthority('UPDATE_USER')")
    @PatchMapping("/{userId}")
    public ResponseEntity<Boolean> updateActive(@PathVariable String userId,
                                                @RequestParam boolean active) {
        return ResponseEntity.ok(userService.updateActive(userId, active));
    }

    @PreAuthorize("hasAuthority('DELETE_USER')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable String userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }

}
