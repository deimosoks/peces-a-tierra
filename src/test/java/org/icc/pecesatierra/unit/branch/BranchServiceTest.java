package org.icc.pecesatierra.unit.branch;

import org.icc.pecesatierra.features.attendance.repository.AttendanceRepository;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.branch.dtos.BranchRequestDto;
import org.icc.pecesatierra.features.branch.exceptions.CannotDeleteBranchWithRecords;
import org.icc.pecesatierra.features.branch.exceptions.CannotManageBranchOutsideYourBranchException;
import org.icc.pecesatierra.features.branch.mapper.BranchMapper;
import org.icc.pecesatierra.features.branch.repository.BranchRepository;
import org.icc.pecesatierra.features.branch.service.BranchService;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.enums.AppPermission;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;
    @Mock
    private BranchMapper branchMapper;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private DateTimeUtils dateTimeUtils;

    @InjectMocks
    private BranchService branchService;

    private final Branch branchA = TestFixtures.branch("branch-a", "Sede A");
    private final Branch branchB = TestFixtures.branch("branch-b", "Sede B");

    private User operator() {
        return TestFixtures.user("user-op", "operador",
                TestFixtures.member("op-member", branchA, "Operador A"),
                AppPermission.BRANCH_UPDATE.name(), AppPermission.BRANCH_DELETE.name());
    }

    private User administrator() {
        return TestFixtures.user("user-admin", "admin",
                TestFixtures.member("admin-member", branchA, "Admin A"),
                AppPermission.ADMINISTRATOR.name());
    }

    private BranchRequestDto request(String name) {
        return BranchRequestDto.builder()
                .name(name)
                .address("Calle 99 # 8-7")
                .city("Cali")
                .cellphone("3111111111")
                .build();
    }

    private void answerMapperWithRequest() {
        doAnswer(invocation -> {
            BranchRequestDto dto = invocation.getArgument(0);
            Branch branch = invocation.getArgument(1);
            branch.setName(dto.getName());
            branch.setAddress(dto.getAddress());
            branch.setCity(dto.getCity());
            branch.setCellphone(dto.getCellphone());
            return null;
        }).when(branchMapper).updateEntityFromDto(any(BranchRequestDto.class), any(Branch.class));
    }

    @Test
    void updateFromAnotherBranchIsRejectedWithoutMutating_H20() {
        when(branchRepository.findById(branchB.getId())).thenReturn(Optional.of(branchB));

        assertThatThrownBy(() -> branchService.update(request("Sede secuestrada"), branchB.getId(), operator()))
                .isInstanceOfSatisfying(CannotManageBranchOutsideYourBranchException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(branchMapper, never()).updateEntityFromDto(any(BranchRequestDto.class), any(Branch.class));
        verify(branchRepository, never()).save(any(Branch.class));
        assertThat(branchB.getName()).isEqualTo("Sede B");
        assertThat(branchB.getAddress()).isEqualTo("Calle 1 # 2-3");
        assertThat(branchB.getCity()).isEqualTo("Bogota");
        assertThat(branchB.getCellphone()).isEqualTo("3000000000");
    }

    @Test
    void updateOwnBranchIsAllowedForNonAdmin_H20() {
        when(branchRepository.findById(branchA.getId())).thenReturn(Optional.of(branchA));
        answerMapperWithRequest();
        when(branchRepository.save(branchA)).thenReturn(branchA);
        BranchRequestDto request = request("Sede A Renombrada");

        branchService.update(request, branchA.getId(), operator());

        verify(branchMapper).updateEntityFromDto(request, branchA);
        verify(branchRepository).save(branchA);
        assertThat(branchA.getName()).isEqualTo("Sede A Renombrada");
        assertThat(branchA.getCity()).isEqualTo("Cali");
    }

    @Test
    void updateAnotherBranchIsAllowedForAdministrator_H20() {
        when(branchRepository.findById(branchB.getId())).thenReturn(Optional.of(branchB));
        answerMapperWithRequest();
        when(branchRepository.save(branchB)).thenReturn(branchB);
        BranchRequestDto request = request("Sede B Renombrada");

        branchService.update(request, branchB.getId(), administrator());

        verify(branchMapper).updateEntityFromDto(request, branchB);
        verify(branchRepository).save(branchB);
        assertThat(branchB.getName()).isEqualTo("Sede B Renombrada");
    }

    @Test
    void deleteFromAnotherBranchIsRejectedBeforeCheckingRecords_H20() {
        when(branchRepository.findById(branchB.getId())).thenReturn(Optional.of(branchB));

        assertThatThrownBy(() -> branchService.delete(branchB.getId(), operator()))
                .isInstanceOfSatisfying(CannotManageBranchOutsideYourBranchException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verifyNoInteractions(memberRepository, attendanceRepository);
        verify(branchRepository, never()).delete(any(Branch.class));
    }

    @Test
    void deleteAnotherBranchWithRecordsIsRejectedAsScopeViolation_H20() {
        when(branchRepository.findById(branchB.getId())).thenReturn(Optional.of(branchB));
        lenient().when(memberRepository.existsMemberByBranch(branchB)).thenReturn(true);
        lenient().when(attendanceRepository.existsAttendanceByBranch(branchB)).thenReturn(true);

        assertThatThrownBy(() -> branchService.delete(branchB.getId(), operator()))
                .isInstanceOfSatisfying(CannotManageBranchOutsideYourBranchException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .isNotInstanceOf(CannotDeleteBranchWithRecords.class);

        verify(memberRepository, never()).existsMemberByBranch(any(Branch.class));
        verify(attendanceRepository, never()).existsAttendanceByBranch(any(Branch.class));
        verify(branchRepository, never()).delete(any(Branch.class));
    }

    @Test
    void deleteOwnBranchIsAllowedForNonAdmin_H20() {
        when(branchRepository.findById(branchA.getId())).thenReturn(Optional.of(branchA));

        branchService.delete(branchA.getId(), operator());

        verify(branchRepository).delete(branchA);
    }

    @Test
    void deleteAnotherBranchIsAllowedForAdministrator_H20() {
        when(branchRepository.findById(branchB.getId())).thenReturn(Optional.of(branchB));

        branchService.delete(branchB.getId(), administrator());

        verify(branchRepository).delete(branchB);
    }
}
