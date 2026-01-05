package org.icc.pecesatierra.dtos.user;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPagesResponseDto {

    private List<UserResponseDto> users;
    private int pages;

}
