package org.icc.pecesatierra.dtos.member;

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
