package org.icc.pecesatierra.utils.mappers;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.permission.PermissionResponseDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;
import org.icc.pecesatierra.dtos.user.UserResponseDto;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class UserMapper {

    private final MemberMapper memberMapper;
    private final MemberRepository memberRepository;

    //TODO: refactorizar esto
    public UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updateAt(user.getUpdatedAt())
                .branchName(user.getMember().getBranch().getName())
                .memberResponseDto(memberMapper.toDto(user.getMember(), false))
                .roles(user.getRoles().stream().map(userRole -> {
                    Member givenBy = memberRepository.findById(userRole.getGiverId()).orElse(null);
                    return RoleResponseDto.builder()
                            .id(userRole.getRole().getId())
                            .name(userRole.getRole().getName())
                            .color(userRole.getRole().getColor())
                            .createdAt(userRole.getRole().getCreatedAt())
                            .updatedAt(userRole.getRole().getUpdatedAt())
                            .description(userRole.getRole().getDescription())
                            .givenBy(givenBy != null ? givenBy.getCompleteName()
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
