package org.icc.pecesatierra.dtos.auth;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {
    private AccessTokenDto accessTokenDto;
    private RefreshTokenDto refreshTokenDto;
}
