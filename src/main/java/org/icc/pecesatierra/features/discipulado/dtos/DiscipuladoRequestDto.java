package org.icc.pecesatierra.features.discipulado.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscipuladoRequestDto {

    @NotBlank
    private String memberId;

    @NotNull
    private LocalDateTime dateStarted;

}
