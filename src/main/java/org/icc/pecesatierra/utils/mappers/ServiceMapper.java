package org.icc.pecesatierra.utils.mappers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.service.ServiceRequestDto;
import org.icc.pecesatierra.dtos.service.ServiceResponseDto;
import org.icc.pecesatierra.entities.Services;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

//@Mapper(componentModel = "spring")
@Component
@RequiredArgsConstructor
public class ServiceMapper {

//    ServiceResponseDto toDto(Services services);
//
//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
//    @Mapping(target = "active", ignore = true)
//    void updateEntityFromDto(ServiceRequestDto serviceRequestDto, @MappingTarget Services services);

    private final DateTimeUtils dateTimeUtils;

    public ServiceResponseDto toDto(Services services) {
        if (services == null) {
            return null;
        }

        ServiceResponseDto.ServiceResponseDtoBuilder serviceResponseDto = ServiceResponseDto.builder();

        serviceResponseDto.id(services.getId());
        serviceResponseDto.name(services.getName());
        serviceResponseDto.createdAt(dateTimeUtils.toColombia(services.getCreatedAt()));
        serviceResponseDto.description(services.getDescription());
        serviceResponseDto.active(services.isActive());

        return serviceResponseDto.build();
    }

    public void updateEntityFromDto(ServiceRequestDto serviceRequestDto, Services services) {
        if (serviceRequestDto == null) {
            return;
        }

        services.setName(serviceRequestDto.getName());
        services.setDescription(serviceRequestDto.getDescription());
    }

}
