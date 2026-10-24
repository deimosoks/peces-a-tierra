package org.icc.pecesatierra.integration.attendance;

import org.icc.pecesatierra.features.attendance.Attendance;
import org.icc.pecesatierra.features.attendance.dtos.AttendanceFiltersRequestDto;
import org.icc.pecesatierra.features.attendance.dtos.AttendanceResponseDto;
import org.icc.pecesatierra.features.attendance.repository.AttendanceRepository;
import org.icc.pecesatierra.features.attendance.service.AttendanceService;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.branch.repository.BranchRepository;
import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.category.repository.MemberCategoryRepository;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.service.ServiceEvent;
import org.icc.pecesatierra.features.service.Services;
import org.icc.pecesatierra.features.service.repository.ServiceEventRepository;
import org.icc.pecesatierra.features.service.repository.ServiceRepository;
import org.icc.pecesatierra.features.type.MemberType;
import org.icc.pecesatierra.features.type.repository.MemberTypeRepository;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.AbstractIntegrationTest;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.models.ExportResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H-02 contra PostgreSQL real: el export de asistencias sin cuerpo debe quedar
 * acotado a la sede del operador y, solo para un administrador, abarcar todas
 * las sedes. Tambien comprueba que un {@code branchId} ajeno no permite escapar
 * del scoping.
 *
 * <p>Sin Docker la clase se salta (ver {@link AbstractIntegrationTest}).</p>
 */
class AttendanceExportIT extends AbstractIntegrationTest {

    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private BranchRepository branchRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private MemberCategoryRepository memberCategoryRepository;
    @Autowired
    private MemberTypeRepository memberTypeRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private ServiceEventRepository serviceEventRepository;

    private Branch branchA;
    private Branch branchB;
    private User operatorA;
    private User admin;

    @BeforeEach
    void seedData() {
        attendanceRepository.deleteAll();
        serviceEventRepository.deleteAll();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        branchA = branchRepository.save(Branch.builder()
                .name("Sede A " + suffix)
                .address("Calle 1")
                .city("Bogota")
                .cellphone("3000000000")
                .createdAt(TestFixtures.FIXED_NOW)
                .build());
        branchB = branchRepository.save(Branch.builder()
                .name("Sede B " + suffix)
                .address("Calle 2")
                .city("Bogota")
                .cellphone("3000000001")
                .createdAt(TestFixtures.FIXED_NOW)
                .build());

        Member operatorMember = memberRepository.save(Member.builder()
                .completeName("Operador A " + suffix)
                .branch(branchA)
                .active(true)
                .createdAt(TestFixtures.FIXED_NOW)
                .build());
        Member memberA = memberRepository.save(Member.builder()
                .completeName("Integrante A " + suffix)
                .branch(branchA)
                .active(true)
                .createdAt(TestFixtures.FIXED_NOW)
                .build());
        Member memberB = memberRepository.save(Member.builder()
                .completeName("Integrante B " + suffix)
                .branch(branchB)
                .active(true)
                .createdAt(TestFixtures.FIXED_NOW)
                .build());

        MemberCategory category = memberCategoryRepository.save(MemberCategory.builder()
                .name("General " + suffix)
                .color("#FFFFFF")
                .build());
        MemberType type = memberTypeRepository.save(MemberType.builder()
                .name("Miembro " + suffix)
                .color("#FFFFFF")
                .build());
        Services service = serviceRepository.save(Services.builder()
                .name("Culto " + suffix)
                .description("Servicio de prueba")
                .active(true)
                .createdAt(TestFixtures.FIXED_NOW)
                .build());

        ServiceEvent eventA = serviceEventRepository.save(ServiceEvent.builder()
                .services(service)
                .branch(branchA)
                .createdBy(operatorMember)
                .startDateTime(TestFixtures.FIXED_NOW.minusHours(1))
                .endDateTime(TestFixtures.FIXED_NOW.plusHours(1))
                .build());
        ServiceEvent eventB = serviceEventRepository.save(ServiceEvent.builder()
                .services(service)
                .branch(branchB)
                .createdBy(operatorMember)
                .startDateTime(TestFixtures.FIXED_NOW.minusHours(1))
                .endDateTime(TestFixtures.FIXED_NOW.plusHours(1))
                .build());

        attendanceRepository.saveAll(List.of(
                Attendance.builder()
                        .member(memberA)
                        .serviceEvent(eventA)
                        .branch(branchA)
                        .attendanceDate(TestFixtures.FIXED_NOW)
                        .memberCategory(category)
                        .memberType(type)
                        .registeredById(operatorMember)
                        .invalid(false)
                        .build(),
                Attendance.builder()
                        .member(memberB)
                        .serviceEvent(eventB)
                        .branch(branchB)
                        .attendanceDate(TestFixtures.FIXED_NOW)
                        .memberCategory(category)
                        .memberType(type)
                        .registeredById(operatorMember)
                        .invalid(false)
                        .build()
        ));

        operatorA = TestFixtures.user("user-operator-a", "operador-a", operatorMember);
        admin = TestFixtures.user("user-admin", "admin", operatorMember, "ADMINISTRATOR");
    }

    @Test
    void exportWithoutBodyOnlyReturnsTheOperatorsBranch_H02() {
        ExportResponseDto<AttendanceResponseDto> response = attendanceService.export(null, operatorA);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getData())
                .extracting(AttendanceResponseDto::getBranchName)
                .containsExactly(branchA.getName());
    }

    @Test
    void adminExportWithoutBodyReturnsEveryBranch_H02() {
        ExportResponseDto<AttendanceResponseDto> response = attendanceService.export(null, admin);

        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getData())
                .extracting(AttendanceResponseDto::getBranchName)
                .containsExactlyInAnyOrder(branchA.getName(), branchB.getName());
    }

    @Test
    void operatorCannotEscapeOwnBranchWithAForeignBranchId_H02() {
        AttendanceFiltersRequestDto dto = AttendanceFiltersRequestDto.builder()
                .branchId(branchB.getId())
                .build();

        ExportResponseDto<AttendanceResponseDto> response = attendanceService.export(dto, operatorA);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getData())
                .extracting(AttendanceResponseDto::getBranchName)
                .containsExactly(branchA.getName());
    }
}
