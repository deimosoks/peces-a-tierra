package org.icc.pecesatierra.features.type.dtos;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberTypeResponseDto {
    private String id;
    private String name;
    private String color;
}
