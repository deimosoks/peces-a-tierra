package org.icc.pecesatierra.dtos.auth;

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
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
