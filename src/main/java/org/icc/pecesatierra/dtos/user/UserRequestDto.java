package org.icc.pecesatierra.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.icc.pecesatierra.enums.CategoryPerson;
import org.icc.pecesatierra.enums.TypePerson;

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
    private String memberCompleteName;

    @NotBlank
    private String cellphone;

    @NotBlank
    private String address;

    @NotBlank
    private String birthdate;

    @NotNull
    private CategoryPerson categoryPerson;

    @NotNull
    private TypePerson typePerson;

    @NotBlank
    private String cc;

}
