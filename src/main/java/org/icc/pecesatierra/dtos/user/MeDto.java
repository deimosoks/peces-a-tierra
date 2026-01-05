package org.icc.pecesatierra.dtos.user;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeDto {

    private String username;
    private List<String> permissions;

}
