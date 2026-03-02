//package org.icc.pecesatierra.web.controllers;
//
//import jakarta.validation.Valid;
//import lombok.AllArgsConstructor;
//import org.icc.pecesatierra.domain.entities.User;
//import org.icc.pecesatierra.dtos.user.UserRequestDto;
//import org.icc.pecesatierra.web.services.UserService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/users2")
//@AllArgsConstructor
//public class UserController2 extends BaseController {
//
//    private final UserService userService;
//
//    // TODO: Usa este controlador como ejemplo para guiarte estandarizando las responses, luego eliminalo gorrrrrrdito
//
//    @PreAuthorize("hasAuthority('VIEW_USER_PANEL')")
//    @PostMapping("/report")
//    public ResponseEntity<?> report() {
//        return success(userService.report());
//    }
//
//    @PreAuthorize("hasAuthority('CREATE_USER')")
//    @PostMapping
//    public ResponseEntity<?> create(@RequestBody @Valid UserRequestDto userRequestDto,
//                                    @AuthenticationPrincipal User givenBy) {
//        return created(userService.create(userRequestDto, givenBy));
//    }
//
//    @GetMapping("/@me")
//    public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
//        return success(userService.me(user));
//    }
//
////    @PreAuthorize("hasAuthority('VIEW_USER_PANEL')")
////    @GetMapping
////    public ResponseEntity<?> findAll(@RequestParam int page) {
////        return success(userService.findAll(page));
////    }
//
//    @PreAuthorize("hasAuthority('VIEW_USER_PANEL')")
//    @GetMapping("/query")
//    public ResponseEntity<?> findAllByQuery(@RequestParam int page,
//                                            @RequestParam String query) {
//        return success(userService.findByQuery(query, page));
//    }
//
//    @PreAuthorize("hasAuthority('UPDATE_USER')")
//    @PutMapping("/{userId}")
//    public ResponseEntity<?> update(@RequestBody UserRequestDto userRequestDto,
//                                    @PathVariable String userId,
//                                    @AuthenticationPrincipal User user) {
//        return success(userService.update(userRequestDto, userId, user));
//    }
//
//    @PreAuthorize("hasAuthority('UPDATE_USER')")
//    @PatchMapping("/{userId}")
//    public ResponseEntity<?> updateActive(@PathVariable String userId,
//                                          @RequestParam boolean active) {
//        return success(userService.updateActive(userId, active));
//    }
//
//    @PreAuthorize("hasAuthority('DELETE_USER')")
//    @DeleteMapping("/{userId}")
//    public ResponseEntity<?> delete(@PathVariable String userId) {
//        return noContent(() -> userService.delete(userId));
//    }
//}
