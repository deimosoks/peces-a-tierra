package org.icc.pecesatierra.dtos.user;

import lombok.*;
import org.icc.pecesatierra.enums.CategoryPerson;
import org.icc.pecesatierra.enums.TypePerson;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private String memberId;
    private String username;
    private String memberCompleteName;
    private String cellphone;
    private String address;
    private String birthdate;
    private int age;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
    private boolean state;
    private CategoryPerson categoryPerson;
    private TypePerson typePerson;
    private String cc;

}
