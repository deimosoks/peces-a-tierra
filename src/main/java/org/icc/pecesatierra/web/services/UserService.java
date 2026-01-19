package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.dtos.user.*;

public interface UserService {

    UserResponseDto create(UserRequestDto userRequestDto, User givenBy);

    MeDto me(User user);

    UserPagesResponseDto findAll(int page, String query);

    UserResponseDto update(UserRequestDto userRequestDto, String userId, User givenBy);

    boolean updateActive(User user,String userId, boolean active);

    void delete(String userId);

    UserReportResponseDto report();

}
