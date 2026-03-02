package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.dtos.user.*;
import org.icc.pecesatierra.utils.models.PagesResponseDto;

public interface UserService {

    UserResponseDto create(UserRequestDto userRequestDto, User givenBy);

    MeDto me(User user);

    PagesResponseDto<UserResponseDto> search(int page, String query, User user, String branchId);

    UserResponseDto update(UserRequestDto userRequestDto, String userId, User givenBy);

    boolean updateActive(User user,String userId, boolean active);

    void delete(User user, String userId);

    UserReportResponseDto report(User user);

}
