package org.icc.pecesatierra.dtos.auth;

import lombok.*;

import java.util.Date;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshTokenDto {
    private String token;
    private Date expiresAt;
}
