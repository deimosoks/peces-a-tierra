package org.icc.pecesatierra.controllers;

import org.icc.pecesatierra.dtos.user.*;
import org.icc.pecesatierra.entities.User;
import org.springframework.http.ResponseEntity;

public interface UserController {

    ResponseEntity<UserReportResponseDto> report();

    ResponseEntity<UserResponseDto> create(UserRequestDto userRequestDto, User givenBy);

    ResponseEntity<MeDto> me(User user);

    ResponseEntity<UserPagesResponseDto> findAll(int page);

    ResponseEntity<UserPagesResponseDto> findAllByQuery(int page, String query);

    ResponseEntity<UserResponseDto> update(UserRequestDto userRequestDto, String userId ,User user);

    ResponseEntity<Boolean> updateActive(String userId, boolean active);

    ResponseEntity<Void> delete(String userId);
}
