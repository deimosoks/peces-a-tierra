package org.icc.pecesatierra.dtos.member.category;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberCategoryRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    private String color;

}
