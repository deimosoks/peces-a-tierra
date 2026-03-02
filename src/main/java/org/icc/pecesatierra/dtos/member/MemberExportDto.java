package org.icc.pecesatierra.dtos.member;


import lombok.*;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberExportDto {

    private String completeName;
    private String type;
    private String category;
    private String cellphone;
    private String subCategory;
    private String address;
    private LocalDate birthdate;
    private Integer age;
    private String cc;
    private String gender;

}
