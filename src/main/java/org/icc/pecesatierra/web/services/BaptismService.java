package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.baptism.*;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.utils.models.PagesResponseDto;

public interface BaptismService {

    BaptismResponseDto create(BaptismRequestDto baptismRequestDto, User user);
    BaptismResponseDto invalid(BaptismInvalidRequestDto baptismInvalidRequestDto, User user);
    PagesResponseDto<BaptismResponseDto> search(int page, BaptismFilterRequestDto baptismFilterRequestDto, User user);

}
