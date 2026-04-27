package org.icc.pecesatierra.dtos.discipulado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscipuladoProgressRequestDto {

    @NotBlank
    private String discipuladoProgressId;

    @NotBlank
    private String teacherId;

    @NotNull
    private LocalDateTime dateCompleted;

}
