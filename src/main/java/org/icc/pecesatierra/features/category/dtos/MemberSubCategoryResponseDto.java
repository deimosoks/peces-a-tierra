package org.icc.pecesatierra.features.category.dtos;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberSubCategoryResponseDto {

    private String id;
    private String name;
    private String color;

}
