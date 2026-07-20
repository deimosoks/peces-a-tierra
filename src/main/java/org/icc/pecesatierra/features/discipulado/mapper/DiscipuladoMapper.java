package org.icc.pecesatierra.features.discipulado.mapper;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.discipulado.dtos.DiscipuladoResponseDto;
import org.icc.pecesatierra.features.discipulado.Discipulado;
import org.icc.pecesatierra.features.category.repository.mapper.MemberMapper;
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
                .branch(discipulado.getMember().getBranch().getName())
                .progress(discipulado.getProgress().stream().map(discipuladoProgressMapper::toDto).toList())
                .build();
    }

}
