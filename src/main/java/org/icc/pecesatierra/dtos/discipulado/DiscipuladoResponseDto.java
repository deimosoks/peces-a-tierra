package org.icc.pecesatierra.dtos.discipulado;

import lombok.*;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscipuladoResponseDto {

    private String id;
    private MemberResponseDto member;
    private LocalDateTime dateStarted;
    private String registeredBy;
    private LocalDateTime createdAt;
    private List<DiscipuladoProgressResponseDto> progress;

}
