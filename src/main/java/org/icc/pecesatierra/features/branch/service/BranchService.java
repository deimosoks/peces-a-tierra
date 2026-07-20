package org.icc.pecesatierra.features.branch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.features.branch.dtos.BranchRequestDto;
import org.icc.pecesatierra.features.branch.dtos.BranchResponseDto;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.branch.exceptions.CannotDeleteBranchWithRecords;
import org.icc.pecesatierra.features.branch.exceptions.BranchNotFoundException;
import org.icc.pecesatierra.features.attendance.repository.AttendanceRepository;
import org.icc.pecesatierra.features.branch.repository.BranchRepository;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.branch.mapper.BranchMapper;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;
    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;
    private final DateTimeUtils dateTimeUtils;

    @Transactional
    public BranchResponseDto create(BranchRequestDto branchRequestDto, User user) {
        Branch branch = Branch.builder()
                .name(branchRequestDto.getName())
                .address(branchRequestDto.getAddress())
                .city(branchRequestDto.getCity())
                .createdAt(dateTimeUtils.nowUTC())
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
    public List<BranchResponseDto> findAll() {
        return branchRepository.findAll()
                .stream()
                .map(branchMapper::toDto)
                .toList();
    }
}