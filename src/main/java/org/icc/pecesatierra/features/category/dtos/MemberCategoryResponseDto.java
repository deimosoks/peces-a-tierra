package org.icc.pecesatierra.features.category.dtos;

import lombok.*;

import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberCategoryResponseDto {

    private String id;
    private String name;
    private String color;
    private Set<MemberSubCategoryResponseDto> subCategories;

}
