package org.icc.pecesatierra.utils.mappers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.discipulado.DiscipuladoProgressResponseDto;
import org.icc.pecesatierra.entities.DiscipuladoProgress;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiscipuladoProgressMapper {

    private final DiscipuladoMapper discipuladoMapper;
    private final DateTimeUtils dateTimeUtils;

    public DiscipuladoProgressResponseDto toDto(DiscipuladoProgress discipuladoProgress) {
        return DiscipuladoProgressResponseDto.builder()
                .id(discipuladoProgress.getId())
                .discipulado(discipuladoMapper.toDto(discipuladoProgress.getDiscipulado()))
                .step(discipuladoProgress.getStep())
                .teacher(discipuladoProgress.getTeacherId().getCompleteName())
                .completed(discipuladoProgress.isCompleted())
                .dateCompleted(dateTimeUtils.toColombia(discipuladoProgress.getDateCompleted()))
                .registeredBy(discipuladoProgress.getRegisteredBy().getCompleteName())
                .build();
    }

}
