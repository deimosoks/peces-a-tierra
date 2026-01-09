package org.icc.pecesatierra.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {

    @NotBlank
    private String memberId;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotNull
    private Set<String> rolesId;


//    @NotBlank
//    private String memberCompleteName;
//
//    @NotBlank
//    private String cellphone;
//
//    @NotBlank
//    private String address;
//
//    @NotBlank
//    private String birthdate;
//
//    @NotNull
//    private CategoryMember categoryMember;
//
//    @NotNull
//    private TypeMember typeMember;
//

//
//    @NotBlank
//    private String cc;
}
