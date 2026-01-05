package org.icc.pecesatierra.dtos.member;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberPagesResponseDto {

    private List<MemberResponseDto> members;
    private int pages;

}
