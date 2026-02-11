package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.baptism.*;
import org.icc.pecesatierra.entities.User;

public interface BaptismService {

    BaptismResponseDto create(BaptismRequestDto baptismRequestDto, User user);
    BaptismResponseDto invalid(BaptismInvalidRequestDto baptismInvalidRequestDto, User user);
    BaptismPagesResponseDto findAll(int page, BaptismFilterRequestDto baptismFilterRequestDto);

}
