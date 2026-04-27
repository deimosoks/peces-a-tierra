package org.icc.pecesatierra.dtos.discipulado;

import lombok.*;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.entities.Discipulado;

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
