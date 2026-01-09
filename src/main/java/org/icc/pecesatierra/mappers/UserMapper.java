package org.icc.pecesatierra.mappers;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.permission.PermissionResponseDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;
import org.icc.pecesatierra.dtos.user.UserResponseDto;
import org.icc.pecesatierra.entities.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

//@Mapper(componentModel = "spring")
@Component
@AllArgsConstructor
public class UserMapper {

    private final MemberMapper memberMapper;

    public UserResponseDto toDto(User user){
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updateAt(user.getUpdatedAt())
                .memberResponseDto(memberMapper.toDto(user.getMember()))
                .roles(user.getRoles().stream().map(userRole ->
                        RoleResponseDto.builder()
                                .id(userRole.getRole().getId())
                                .name(userRole.getRole().getName())
                                .color(userRole.getRole().getColor())
                                .createdAt(userRole.getRole().getCreatedAt())
                                .updatedAt(userRole.getRole().getUpdatedAt())
                                .description(userRole.getRole().getDescription())
                                .givenById(userRole.getGiverId())
                                .permissions(userRole.getRole().getPermissions().stream().map(rolePermission ->
                                        PermissionResponseDto.builder()
                                                .name(rolePermission.getPermission().getName())
                                                .build()).collect(Collectors.toUnmodifiableSet())
                                )
                                .build()
                ).collect(Collectors.toSet()))
                .build();

    }

//    @Mapping(target = "roles", ignore = true)
//    UserResponseDto toDto(User user);
//
//    @Mapping(target = "roles", ignore = true)
//    @Mapping(target = "passwordHash", ignore = true)
//    @Mapping(target = "memberResponseDto", ignore = true)
//    void updateEntityFromDto(UserRequestDto userRequestDto, @MappingTarget User user);

}
