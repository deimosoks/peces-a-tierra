package org.icc.pecesatierra.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {

    @NotBlank(message = "Debe ingresar un id de miembro valido.")
    private String memberId;

    @NotBlank(message = "Debe ingresar un nombre de usuario valido.")
    @Size(min = 5, max = 20, message = "El nombre de usuario debe tener entre 3 y 20 caracteres.")
    private String username;

//    @NotBlank(message = "Debe ingresar una contraseña valida.")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
    private String password;

    @NotNull(message = "Debe ingresar un id de rol valido.")
    private Set<String> rolesId;
}
