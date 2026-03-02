package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.branch.BranchRequestDto;
import org.icc.pecesatierra.dtos.branch.BranchResponseDto;
import org.icc.pecesatierra.entities.Branch;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.branches.CannotDeleteBranchWithRecords;
import org.icc.pecesatierra.exceptions.branches.BranchNotFoundException;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.BranchRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.utils.mappers.BranchMapper;
import org.icc.pecesatierra.web.services.BranchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;
    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;

    @Transactional
    @Override
    public BranchResponseDto create(BranchRequestDto branchRequestDto, User user) {
        Branch branch = Branch.builder()
                .name(branchRequestDto.getName())
                .address(branchRequestDto.getAddress())
                .city(branchRequestDto.getCity())
                .createdAt(LocalDateTime.now())
                .cellphone(branchRequestDto.getCellphone())
                .build();

        branchRepository.save(branch);

        log.info("""
                        Usuario {} creó la sede:
                        ID: {}
                        Nombre: {}
                        Ciudad: {}
                        Dirección: {}
                        Celular: {}
                        """, user.getMember().getId(),
                branch.getId(),
                branch.getName(),
                branch.getCity(),
                branch.getAddress(),
                branch.getCellphone()
        );

        return branchMapper.toDto(branch);
    }

    @Transactional
    @Override
    public BranchResponseDto update(BranchRequestDto branchRequestDto, String branchId, User user) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(BranchNotFoundException::new);

        Branch beforeUpdate = Branch.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .city(branch.getCity())
                .cellphone(branch.getCellphone())
                .createdAt(branch.getCreatedAt())
                .build();

        branchMapper.updateEntityFromDto(branchRequestDto, branch);

        log.info("""
                        Usuario {} actualizó la sede {}.
                        Estado anterior:
                        Nombre: {}
                        Dirección: {}
                        Ciudad: {}
                        Celular: {}
                        Nuevo estado:
                        Nombre: {}
                        Dirección: {}
                        Ciudad: {}
                        Celular: {}
                        """,
                user.getMember().getId(),
                beforeUpdate.getId(),
                beforeUpdate.getName(),
                beforeUpdate.getAddress(),
                beforeUpdate.getCity(),
                beforeUpdate.getCellphone(),
                branch.getName(),
                branch.getAddress(),
                branch.getCity(),
                branch.getCellphone()
        );

        return branchMapper.toDto(branchRepository.save(branch));
    }

    @Transactional
    @Override
    public void delete(String branchId, User user) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(BranchNotFoundException::new);

        if (memberRepository.existsMemberByBranch(branch) || attendanceRepository.existsAttendanceByBranch(branch)) {
            log.warn("Usuario {} intentó eliminar la sede {} pero contiene registros asociados", user.getMember().getId(), branch.getId());
            throw new CannotDeleteBranchWithRecords();
        }

        log.info("""
                        Usuario {} eliminó la sede:
                        ID: {}
                        Nombre: {}
                        Ciudad: {}
                        Dirección: {}
                        Celular: {}
                        """,
                user.getMember().getId(),
                branch.getId(),
                branch.getName(),
                branch.getCity(),
                branch.getAddress(),
                branch.getCellphone()
        );

        branchRepository.delete(branch);
    }

    @Transactional(readOnly = true)
    @Override
    public List<BranchResponseDto> findAll() {
        return branchRepository.findAll()
                .stream()
                .map(branchMapper::toDto)
                .toList();
    }
}