package org.icc.pecesatierra.utils.mappers;

import org.icc.pecesatierra.dtos.service.ServiceRequestDto;
import org.icc.pecesatierra.dtos.service.ServiceResponseDto;
import org.icc.pecesatierra.entities.Services;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface ServiceMapper {

    ServiceResponseDto toDto(Services services);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntityFromDto(ServiceRequestDto serviceRequestDto, @MappingTarget Services services);

}
