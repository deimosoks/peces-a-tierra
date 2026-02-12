package org.icc.pecesatierra.dtos.member;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberCategoryResponseDto {

    private String id;
    private String name;
    private String color;

}
