package org.icc.pecesatierra.utils.mappers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.discipulado.DiscipuladoResponseDto;
import org.icc.pecesatierra.entities.Discipulado;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiscipuladoMapper {

    private final DateTimeUtils dateTimeUtils;
    private final MemberMapper memberMapper;
    private final DiscipuladoProgressMapper discipuladoProgressMapper;

    public DiscipuladoResponseDto toDto(Discipulado discipulado) {
        return DiscipuladoResponseDto.builder()
                .id(discipulado.getId())
                .member(memberMapper.toDto(discipulado.getMember(), false))
                .dateStarted(dateTimeUtils.toColombia(discipulado.getDateStarted()))
                .registeredBy(discipulado.getRegisteredBy().getCompleteName())
                .createdAt(dateTimeUtils.toColombia(discipulado.getCreatedAt()))
                .progress(discipulado.getProgress().stream().map(discipuladoProgressMapper::toDto).toList())
                .build();
    }

}
