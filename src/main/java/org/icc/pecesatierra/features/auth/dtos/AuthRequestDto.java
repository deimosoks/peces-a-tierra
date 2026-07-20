package org.icc.pecesatierra.features.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class AuthRequestDto {
    @NotBlank(message = "Username invalido.")
    private String username;

    @NotBlank(message = "Contraseña invalida.")
    private String password;
}
