package org.icc.pecesatierra.dtos.member;

import lombok.*;
import org.icc.pecesatierra.dtos.branch.BranchResponseDto;
import org.icc.pecesatierra.dtos.member.category.MemberCategoryResponseDto;
import org.icc.pecesatierra.dtos.member.category.MemberSubCategoryResponseDto;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.dtos.member.type.MemberTypeResponseDto;

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

    private boolean categoryLocked;

    private BranchResponseDto branch;
}
