package org.icc.pecesatierra.dtos.member.category.rules;

import lombok.*;
import org.icc.pecesatierra.dtos.member.category.MemberCategoryResponseDto;
import org.icc.pecesatierra.dtos.member.category.MemberSubCategoryResponseDto;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRulesResponseDto {

    private String id;
    private Integer minAge;
    private Integer maxAge;
    private String gender;
    private int priority;
    private MemberCategoryResponseDto category;
    private MemberSubCategoryResponseDto subCategory;
    private boolean active;

}
