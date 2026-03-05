package org.icc.pecesatierra.utils.mappers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.branch.BranchRequestDto;
import org.icc.pecesatierra.dtos.branch.BranchResponseDto;
import org.icc.pecesatierra.entities.Branch;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

//@Mapper(componentModel = "spring")
@Component
@RequiredArgsConstructor
public class BranchMapper {

    private final DateTimeUtils dateTimeUtils;

    public BranchResponseDto toDto(Branch branch) {
        if (branch == null) {
            return null;
        }

        BranchResponseDto.BranchResponseDtoBuilder branchResponseDto = BranchResponseDto.builder();

        branchResponseDto.id(branch.getId());
        branchResponseDto.name(branch.getName());
        branchResponseDto.address(branch.getAddress());
        branchResponseDto.city(branch.getCity());
        branchResponseDto.createdAt(dateTimeUtils.toColombia(branch.getCreatedAt()));
        branchResponseDto.cellphone(branch.getCellphone());

        return branchResponseDto.build();
    }

    public void updateEntityFromDto(BranchRequestDto branchRequestDto, Branch branch) {
        if (branchRequestDto == null) {
            return;
        }

        branch.setName(branchRequestDto.getName());
        branch.setAddress(branchRequestDto.getAddress());
        branch.setCity(branchRequestDto.getCity());
        branch.setCellphone(branchRequestDto.getCellphone());
    }

}
