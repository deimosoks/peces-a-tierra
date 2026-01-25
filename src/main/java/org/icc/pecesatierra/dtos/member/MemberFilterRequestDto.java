package org.icc.pecesatierra.dtos.member;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberFilterRequestDto {

    private Set<String> memberType;
    private Set<String> memberCategory;
    private boolean onlyActive;
    private String query;

}
