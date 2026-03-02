package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.dtos.user.*;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.icc.pecesatierra.web.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController extends BaseController {

    private final UserService userService;

    @PreAuthorize("""
            (
            hasAuthority('VIEW_USER_PANEL') 
            ||
             hasAuthority('ADMINISTRATOR') 
             )
             &&
            @securityService.isActive(authentication)
            """)
    @PostMapping("/report")
    public ResponseEntity<UserReportResponseDto> report(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.report(user));
    }

    @PreAuthorize("""
            (
            hasAuthority('CREATE_USER') 
            || 
            hasAuthority('ADMINISTRATOR')
             )
             && 
             @securityService.isActive(authentication)
            """)
    @PostMapping
    public ResponseEntity<UserResponseDto> create(@RequestBody @Valid UserRequestDto userRequestDto,
                                                  @AuthenticationPrincipal User givenBy) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(userRequestDto, givenBy));
    }

    @PreAuthorize("@securityService.isActive(authentication)")
    @GetMapping("/@me")
    public ResponseEntity<MeDto> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.me(user));
    }

    @PreAuthorize("""
            (
            hasAuthority('VIEW_USER_PANEL') 
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)""")
    @GetMapping
    public ResponseEntity<PagesResponseDto<UserResponseDto>> findAll(@RequestParam(
                                                                             required = false,
                                                                             defaultValue = "0"
                                                                     ) int page,
                                                                     @RequestParam(
                                                                             required = false,
                                                                             defaultValue = ""
                                                                     ) String query,
                                                                     @RequestParam(
                                                                             required = false
                                                                     ) String branchId,
                                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.search(page, query, user, branchId));
    }

    @PreAuthorize("""
            (
            hasAuthority('UPDATE_USER') 
            || 
            hasAuthority('ADMINISTRATOR')
             )
             && 
             @securityService.isActive(authentication)
            """)
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseDto> update(@RequestBody @Valid UserRequestDto userRequestDto,
                                                  @PathVariable String userId,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.update(userRequestDto, userId, user));
    }

    @PreAuthorize("""
            (
            hasAuthority('UPDATE_USER') 
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)""")
    @PatchMapping("/{userId}")
    public ResponseEntity<Boolean> updateActive(@PathVariable String userId,
                                                @RequestParam boolean active,
                                                @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.updateActive(user, userId, active));
    }

    @PreAuthorize("""
            (
            hasAuthority('DELETE_USER') 
            || 
            hasAuthority('ADMINISTRATOR')
            ) 
            && 
            @securityService.isActive(authentication)
            """)
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                       @PathVariable String userId) {
        userService.delete(user, userId);
        return ResponseEntity.noContent().build();
    }

}
