package org.icc.pecesatierra.services.imp;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.report.AttendanceReportRowDto;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.dtos.report.ReportSummaryDto;
import org.icc.pecesatierra.entities.Attendance;
import org.icc.pecesatierra.entities.AttendanceId;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.Services;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.ServiceRepository;
import org.icc.pecesatierra.services.ReportService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@AllArgsConstructor
public class ReportServiceImp implements ReportService {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<AttendanceReportRowDto> generate(ReportRequestDto dto) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<AttendanceReportRowDto> cq = cb.createQuery(AttendanceReportRowDto.class);

        Root<Attendance> attendance = cq.from(Attendance.class);
        Join<Attendance, Member> member = attendance.join("member");
        Join<Attendance, Services> service = attendance.join("services");

        List<Predicate> predicates = new ArrayList<>();

        if (dto.getCategories() != null && !dto.getCategories().isEmpty()) {
            predicates.add(attendance.get("memberCategory").in(dto.getCategories()));
        }

        if (dto.getTypePeoples() != null && !dto.getTypePeoples().isEmpty()) {
            predicates.add(attendance.get("memberType").in(dto.getTypePeoples()));
        }

        if (dto.getServiceIds() != null && !dto.getServiceIds().isEmpty()) {
            predicates.add(attendance.get("id").get("serviceId").in(dto.getServiceIds()));
        }

        if (dto.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    attendance.get("id").get("attendanceDate"),
                    dto.getStartDate()
            ));
        }

        if (dto.getEndDate() != null){
            predicates.add(cb.lessThanOrEqualTo(
                    attendance.get("id").get("attendanceDate"),
                    dto.getEndDate()
            ));
        }

        if (dto.getUserId() != null){
            predicates.add(cb.equal(member.get("id"), dto.getUserId()));
        }

        if (dto.isOnlyActive()){
            predicates.add(cb.isTrue(member.get("active")));
        }

        Path<LocalDateTime> attendanceDateTime = attendance.get("id").get("attendanceDate");

        Expression<LocalDate> attendanceDate = cb.function("DATE", LocalDate.class, attendanceDateTime);

        cq.select(cb.construct(
                AttendanceReportRowDto.class,
                attendanceDate,
                service.get("startTime"),
                service.get("name"),
//                service.get("dayOfWeek"),
                attendance.get("memberCategory"),
                attendance.get("memberType"),
                cb.count(attendance)
        ));

        cq.where(predicates.toArray(new Predicate[0]));

        cq.groupBy(
                attendanceDate,
                service.get("startTime"),
                service.get("name"),
//                service.get("dayOfWeek"),
                attendance.get("memberCategory"),
                attendance.get("memberType")
        );

        cq.orderBy(
                cb.asc(attendanceDate),
                cb.asc(service.get("startTime"))
        );

        return em.createQuery(cq).getResultList();

    }


}
