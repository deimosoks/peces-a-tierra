package org.icc.pecesatierra.utils.mappers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.permission.PermissionResponseDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;
import org.icc.pecesatierra.dtos.user.UserResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final MemberMapper memberMapper;
    private final DateTimeUtils dateTimeUtils;

    //TODO: refactorizar esto
    public UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .active(user.isActive())
                .createdAt(dateTimeUtils.toColombia(user.getCreatedAt()))
                .updateAt(dateTimeUtils.toColombia(user.getUpdatedAt()))
                .branchName(user.getMember().getBranch().getName())
                .memberResponseDto(memberMapper.toDto(user.getMember(), false))
                .roles(user.getRoles().stream().map(userRole -> {
                    return RoleResponseDto.builder()
                            .id(userRole.getRole().getId())
                            .name(userRole.getRole().getName())
                            .color(userRole.getRole().getColor())
                            .createdAt(dateTimeUtils.toColombia(userRole.getRole().getCreatedAt()))
                            .updatedAt(dateTimeUtils.toColombia(userRole.getRole().getUpdatedAt()))
                            .description(userRole.getRole().getDescription())
                            .givenBy(userRole.getGiverId() != null ? userRole.getGiverId().getCompleteName()
                                    : "desconocido")
                            .permissions(userRole.getRole().getPermissions().stream()
                                    .map(rolePermission -> PermissionResponseDto
                                            .builder()
                                            .name(rolePermission
                                                    .getId()
                                                    .getPermission())
                                            .build())
                                    .collect(Collectors.toUnmodifiableSet()))
                            .build();
                }).collect(Collectors.toSet()))
                .build();

    }

}
