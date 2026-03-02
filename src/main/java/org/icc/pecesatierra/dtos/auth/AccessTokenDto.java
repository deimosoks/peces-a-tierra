package org.icc.pecesatierra.dtos.auth;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessTokenDto {
    private String token;
    private Date expiredAt;
}
