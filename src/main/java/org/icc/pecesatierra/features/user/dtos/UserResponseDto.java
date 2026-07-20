package org.icc.pecesatierra.features.user.dtos;

import lombok.*;
import org.icc.pecesatierra.features.member.dtos.MemberResponseDto;
import org.icc.pecesatierra.features.role.dtos.RoleResponseDto;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private String id;
    private String username;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
    private MemberResponseDto memberResponseDto;
    private Set<RoleResponseDto> roles;
    private String branchName;

}
