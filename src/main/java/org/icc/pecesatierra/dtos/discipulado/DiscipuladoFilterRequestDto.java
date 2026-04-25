package org.icc.pecesatierra.dtos.discipulado;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscipuladoFilterRequestDto {

    private String query;
    private String teacherId;
    private String memberId;
    private LocalDateTime dateStartedFrom;
    private LocalDateTime dateStartedTo;
    private Boolean completed;

}
