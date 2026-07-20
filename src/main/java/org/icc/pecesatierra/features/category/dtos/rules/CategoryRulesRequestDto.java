package org.icc.pecesatierra.features.category.dtos.rules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.icc.pecesatierra.utils.enums.Gender;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRulesRequestDto {

    private Integer minAge;
    private Integer maxAge;

    private Gender gender;

    @NotNull
    private Integer priority;

    @NotBlank
    private String memberCategoryId;

    private String subCategoryId;

}
