package org.icc.pecesatierra.dtos.member.type;

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
