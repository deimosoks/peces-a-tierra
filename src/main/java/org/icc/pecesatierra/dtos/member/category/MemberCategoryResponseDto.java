package org.icc.pecesatierra.dtos.member.category;

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
