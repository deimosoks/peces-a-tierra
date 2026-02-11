package org.icc.pecesatierra.dtos.baptism;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaptismPagesResponseDto {
    private List<BaptismResponseDto> baptisms;
    private int pages;
}