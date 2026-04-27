package org.icc.pecesatierra.dtos.discipulado;

import lombok.*;
import org.icc.pecesatierra.utils.models.OrderBy;

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
    private OrderBy orderBy;

}
