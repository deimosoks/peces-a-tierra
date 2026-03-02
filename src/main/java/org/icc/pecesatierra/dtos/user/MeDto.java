package org.icc.pecesatierra.dtos.user;

import lombok.*;

import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeDto {

    private String username;
    private String pictureProfileUrl;
    private String completeName;
    private Set<String> permissions;

}
