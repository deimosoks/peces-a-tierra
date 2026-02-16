package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.branch.BranchRequestDto;
import org.icc.pecesatierra.dtos.branch.BranchResponseDto;
import org.icc.pecesatierra.entities.Branch;
import org.icc.pecesatierra.exceptions.BranchNotFoundException;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.BranchRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.utils.mappers.BranchMapper;
import org.icc.pecesatierra.web.services.BranchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;
    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;

    @Transactional
    @Override
    public BranchResponseDto create(BranchRequestDto branchResponseDto) {
        Branch branch = Branch.builder()
                .name(branchResponseDto.getName())
                .address(branchResponseDto.getAddress())
                .city(branchResponseDto.getCity())
                .createdAt(LocalDateTime.now())
                .cellphone(branchResponseDto.getCellphone())
                .build();
        return branchMapper.toDto(branchRepository.save(branch));
    }

    @Transactional
    @Override
    public BranchResponseDto update(BranchRequestDto branchResponseDto, String branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException("Sede no encontrada"));
        branchMapper.updateEntityFromDto(branchResponseDto, branch);
        return branchMapper.toDto(branchRepository.save(branch));
    }

    @Transactional
    @Override
    public void delete(String branchId) {

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException("Sede no encontrada."));

        if (memberRepository.existsMemberByBranch(branch) || attendanceRepository.existsAttendanceByBranch(branch)) {
            throw new BranchNotFoundException("No puede borrar una sede que tiene registros asociados.");
        }

        branchRepository.delete(branch);

    }

    @Transactional(readOnly = true)
    @Override
    public List<BranchResponseDto> findAll() {
        return branchRepository.findAll().stream().map(branchMapper::toDto).toList();
    }
}
