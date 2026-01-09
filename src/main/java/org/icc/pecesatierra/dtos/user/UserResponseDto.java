package org.icc.pecesatierra.dtos.user;

import lombok.*;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;

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

//    private String memberCompleteName;
//    private String cellphone;
//    private String address;
//    private String birthdate;
//    private CategoryMember categoryMember;
//    private TypeMember typeMember;
//    private String cc;
//    private String memberId;

}
