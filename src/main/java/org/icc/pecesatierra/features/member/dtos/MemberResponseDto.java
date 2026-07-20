package org.icc.pecesatierra.features.member.dtos;

import lombok.*;
import org.icc.pecesatierra.features.branch.dtos.BranchResponseDto;
import org.icc.pecesatierra.features.category.dtos.MemberCategoryResponseDto;
import org.icc.pecesatierra.features.category.dtos.MemberSubCategoryResponseDto;
import org.icc.pecesatierra.features.member.dtos.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.features.type.dtos.MemberTypeResponseDto;

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
    private String registeredBy;

    private boolean categoryLocked;

    private BranchResponseDto branch;
}
