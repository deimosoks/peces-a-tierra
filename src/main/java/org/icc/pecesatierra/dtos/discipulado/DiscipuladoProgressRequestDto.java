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

    @NotNull
    private int step;

    @NotBlank
    private String teacherId;

//    @NotNull
//    private boolean completed;

    @NotNull
    private LocalDateTime dateCompleted;

}
