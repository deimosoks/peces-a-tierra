package org.icc.pecesatierra.web.services;


import org.icc.pecesatierra.dtos.branch.BranchRequestDto;
import org.icc.pecesatierra.dtos.branch.BranchResponseDto;

import java.util.List;

public interface BranchService {

    BranchResponseDto create(BranchRequestDto branchResponseDto);

    BranchResponseDto update(BranchRequestDto branchResponseDto, String branchId);

    void delete(String branchId);

    List<BranchResponseDto> findAll();

}
