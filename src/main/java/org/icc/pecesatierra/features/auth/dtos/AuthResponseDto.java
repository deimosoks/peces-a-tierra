package org.icc.pecesatierra.features.auth.dtos;

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
