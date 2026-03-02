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
    private Set<String> subCategory;
    private String query;
    private Boolean onlyActive;
    private Boolean hasCc;
    private Boolean hasCellphone;
    private Boolean hasAddress;
    private Boolean hasBirthdate;
    private Integer ageFilterRange1;
    private Integer ageFilterRange2;
    private String gender;
    private String location;
    private String branchId;
}
