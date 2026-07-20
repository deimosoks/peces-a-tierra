package org.icc.pecesatierra.features.discipulado.dtos;

import lombok.*;
import org.icc.pecesatierra.features.member.dtos.MemberResponseDto;

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
    private String branch;
    private List<DiscipuladoProgressResponseDto> progress;

}
