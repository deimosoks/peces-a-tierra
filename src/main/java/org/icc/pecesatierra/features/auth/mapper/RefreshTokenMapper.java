package org.icc.pecesatierra.features.auth.mapper;

import org.icc.pecesatierra.features.auth.dtos.RefreshTokenDto;
import org.icc.pecesatierra.features.auth.RefreshToken;
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
