package org.icc.pecesatierra.web.services;


import org.icc.pecesatierra.dtos.branch.BranchRequestDto;
import org.icc.pecesatierra.dtos.branch.BranchResponseDto;
import org.icc.pecesatierra.entities.User;

import java.util.List;

public interface BranchService {

    BranchResponseDto create(BranchRequestDto branchResponseDto, User user);

    BranchResponseDto update(BranchRequestDto branchResponseDto, String branchId, User user);

    void delete(String branchId, User user);

    List<BranchResponseDto> findAll();

}
