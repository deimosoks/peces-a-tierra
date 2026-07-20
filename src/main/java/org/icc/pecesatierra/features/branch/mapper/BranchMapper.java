package org.icc.pecesatierra.features.branch.mapper;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.branch.dtos.BranchRequestDto;
import org.icc.pecesatierra.features.branch.dtos.BranchResponseDto;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
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
