package org.icc.pecesatierra.unit.attendance;

import org.icc.pecesatierra.features.attendance.Attendance;
import org.icc.pecesatierra.features.attendance.dtos.AttendanceRequestDto;
import org.icc.pecesatierra.features.attendance.exceptions.MultipleServiceEventsInBatchException;
import org.icc.pecesatierra.features.attendance.mapper.AttendanceMapper;
import org.icc.pecesatierra.features.attendance.repository.AttendanceRepository;
import org.icc.pecesatierra.features.attendance.service.AttendanceService;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.service.ServiceEvent;
import org.icc.pecesatierra.features.service.Services;
import org.icc.pecesatierra.features.service.repository.ServiceEventRepository;
import org.icc.pecesatierra.features.type.MemberType;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.specs.AttendanceSpecification;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * H-41: un lote de asistencias solo puede pertenecer a un evento de servicio.
 * Antes el servicio leia el evento del primer item y guardaba todas las filas
 * con ese evento aunque los demas trajeran otro distinto.
 *
 * <p>Aqui se comprueba el rechazo del lote mixto y que el lote homogeneo sigue
 * guardando todas las filas con el evento solicitado. No toca base de datos.</p>
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private AttendanceMapper attendanceMapper;
    @Mock
    private ServiceEventRepository serviceEventRepository;
    @Mock
    private AttendanceSpecification attendanceSpecification;
    @Mock
    private DateTimeUtils dateTimeUtils;

    @InjectMocks
    private AttendanceService attendanceService;

    private final Branch branch = TestFixtures.branch("branch-a", "Sede A");
    private final MemberCategory category = MemberCategory.builder()
            .id("cat-1").name("Categoria").color("#112233").build();
    private final MemberType type = MemberType.builder()
            .id("type-1").name("Tipo").color("#112233").build();
    private final User operator = TestFixtures.user("user-1", "operador-a",
            TestFixtures.member("operator-member", branch, "Operador A"));

    private ServiceEvent eventOne;
    private ServiceEvent eventTwo;

    @BeforeEach
    void setUp() {
        eventOne = event("event-1", "Culto dominical");
        eventTwo = event("event-2", "Reunion de jovenes");

        lenient().when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
    }

    @Test
    void batchWithDifferentServiceEventsIsRejectedWith400_H41() {
        List<AttendanceRequestDto> batch = List.of(
                request("event-1", "member-1", null),
                request("event-2", "member-2", null));

        assertThatThrownBy(() -> attendanceService.create(batch, operator))
                .isInstanceOfSatisfying(MultipleServiceEventsInBatchException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(serviceEventRepository, never()).findById(anyString());
        verify(attendanceRepository, never()).saveAll(any());
    }

    @Test
    void batchWithTheSameServiceEventSavesEveryRowWithThatEvent_H41() {
        Member first = member("member-1", "Ana Perez");
        Member second = member("member-2", "Beto Perez");
        when(serviceEventRepository.findById("event-1")).thenReturn(Optional.of(eventOne));
        when(memberRepository.findAllById(any())).thenReturn(List.of(first, second));
        when(attendanceRepository.findMemberIdsByServiceEventIdInvalidFalse("event-1")).thenReturn(new HashSet<>());

        attendanceService.create(List.of(
                request("event-1", "member-1", "Primera"),
                request("event-1", "member-2", "Segunda")), operator);

        ArgumentCaptor<Collection<Attendance>> captor = savedAttendances();
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
                .allSatisfy(attendance -> assertThat(attendance.getServiceEvent()).isSameAs(eventOne));
    }

    @Test
    void batchWithASingleItemSavesItWithItsEvent_H41() {
        Member first = member("member-1", "Ana Perez");
        when(serviceEventRepository.findById("event-2")).thenReturn(Optional.of(eventTwo));
        when(memberRepository.findAllById(any())).thenReturn(List.of(first));
        when(attendanceRepository.findMemberIdsByServiceEventIdInvalidFalse("event-2")).thenReturn(new HashSet<>());

        attendanceService.create(List.of(request("event-2", "member-1", "Unica")), operator);

        ArgumentCaptor<Collection<Attendance>> captor = savedAttendances();
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().iterator().next().getServiceEvent()).isSameAs(eventTwo);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Collection<Attendance>> savedAttendances() {
        ArgumentCaptor<Collection<Attendance>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(attendanceRepository).saveAll(captor.capture());
        return captor;
    }

    private ServiceEvent event(String id, String serviceName) {
        return ServiceEvent.builder()
                .id(id)
                .services(Services.builder()
                        .id("service-" + id).name(serviceName).description("Servicio")
                        .active(true).createdAt(TestFixtures.FIXED_NOW).build())
                .branch(branch)
                .startDateTime(TestFixtures.FIXED_NOW.minusHours(1))
                .endDateTime(TestFixtures.FIXED_NOW.plusHours(1))
                .build();
    }

    private Member member(String id, String name) {
        return Member.builder()
                .id(id)
                .completeName(name)
                .branch(branch)
                .categoryId(category)
                .typeId(type)
                .active(true)
                .createdAt(TestFixtures.FIXED_NOW)
                .build();
    }

    private AttendanceRequestDto request(String serviceEventId, String memberId, String note) {
        return AttendanceRequestDto.builder()
                .serviceEventId(serviceEventId)
                .memberId(memberId)
                .serviceDate(LocalDateTime.of(2026, 1, 15, 7, 0))
                .attendanceDate(LocalDateTime.of(2026, 1, 15, 7, 5))
                .note(note)
                .build();
    }
}
