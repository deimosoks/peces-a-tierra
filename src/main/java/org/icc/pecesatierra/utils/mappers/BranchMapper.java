package org.icc.pecesatierra.utils.mappers;

import org.icc.pecesatierra.dtos.branch.BranchRequestDto;
import org.icc.pecesatierra.dtos.branch.BranchResponseDto;
import org.icc.pecesatierra.entities.Branch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface BranchMapper {

    BranchResponseDto toDto(Branch branch);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(BranchRequestDto branchRequestDto, @MappingTarget Branch branch);

}
