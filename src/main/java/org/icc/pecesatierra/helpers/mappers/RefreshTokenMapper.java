package org.icc.pecesatierra.helpers.mappers;

import org.icc.pecesatierra.dtos.auth.RefreshTokenDto;
import org.icc.pecesatierra.domain.reference.RefreshToken;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface RefreshTokenMapper {

//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "user", ignore = true)
    RefreshTokenDto toDto(RefreshToken refreshToken);

}
