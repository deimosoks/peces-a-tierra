package org.icc.pecesatierra.features.category.dtos;

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
