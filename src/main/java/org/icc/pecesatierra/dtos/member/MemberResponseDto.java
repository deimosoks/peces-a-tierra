package org.icc.pecesatierra.dtos.member;

import lombok.*;
import org.icc.pecesatierra.dtos.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.utils.enums.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponseDto {

    private String id;
    private String completeName;
//    private TypeMember type;
//    private CategoryMember category;

    private MemberTypeResponseDto type;
    private MemberCategoryResponseDto category;

    private String cellphone;
    private String address;
    private LocalDate birthdate;
    private String cc;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String gender;
    private String pictureProfileUrl;
    private Integer age;
    private boolean active;
    private MemberSubCategoryResponseDto subCategory;
    private Set<MemberNoteResponseDto> notes;

}
