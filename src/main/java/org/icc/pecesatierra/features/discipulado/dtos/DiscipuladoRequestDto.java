package org.icc.pecesatierra.features.discipulado.dtos;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank
    private LocalDateTime dateStarted;

}
