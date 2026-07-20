package org.icc.pecesatierra.features.discipulado.dtos;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscipuladoProgressResponseDto {

    private String id;
    private int step;
    private String teacher;
    private boolean completed;
    private LocalDateTime dateCompleted;
    private String registeredBy;
    private LocalDateTime createdAt;

}
