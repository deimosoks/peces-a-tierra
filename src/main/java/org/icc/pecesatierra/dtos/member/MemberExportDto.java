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
    private String cc;

    //address
    private String neighborhood;
    private String city;
    private String municipality;
    private String district;
    private String postalCode;
    private String latitude;
    private String longitude;

}
