package org.icc.pecesatierra.features.category.dtos.rules;

import lombok.*;
import org.icc.pecesatierra.features.category.dtos.MemberCategoryResponseDto;
import org.icc.pecesatierra.features.category.dtos.MemberSubCategoryResponseDto;

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
