package org.icc.pecesatierra.dtos.member.type;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberTypeRequestDto {
    @NotBlank
    private String name;

    @NotBlank
    private String color;
}
