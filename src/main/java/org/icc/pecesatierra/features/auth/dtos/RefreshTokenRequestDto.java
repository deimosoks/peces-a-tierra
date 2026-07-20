package org.icc.pecesatierra.features.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequestDto {
    @NotBlank(message = "Debe ingresar un token valido.")
    private String refreshToken;
}
