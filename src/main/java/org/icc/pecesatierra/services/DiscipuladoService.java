package org.icc.pecesatierra.services;

import org.icc.pecesatierra.dtos.discipulado.*;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.utils.models.PagesResponseDto;

public interface DiscipuladoService {

    DiscipuladoResponseDto create(DiscipuladoRequestDto dto, User user);

    DiscipuladoResponseDto findById(String id);

    PagesResponseDto<DiscipuladoResponseDto> search(int page, DiscipuladoFilterRequestDto filters, User user);

    void delete(String id);

    DiscipuladoProgressResponseDto progress(DiscipuladoProgressRequestDto dto, User user);

}
