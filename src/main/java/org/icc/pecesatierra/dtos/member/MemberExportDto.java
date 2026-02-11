package org.icc.pecesatierra.dtos.member;


import lombok.*;
import org.icc.pecesatierra.utils.enums.CategoryMember;
import org.icc.pecesatierra.utils.enums.TypeMember;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberExportDto {

    private String completeName;
    private TypeMember type;
    private CategoryMember category;
    private String cellphone;
    private String address;
    private LocalDate birthdate;
    private Integer age;
    private String cc;

}
