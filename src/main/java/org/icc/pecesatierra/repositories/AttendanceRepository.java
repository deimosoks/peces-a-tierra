package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.Attendance;
import org.icc.pecesatierra.entities.AttendanceId;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, AttendanceId> {

//    @Query("""
//           SELECT new org.icc.pecesatierra.dtos.report.ReportSummaryDto(m.category, COUNT(a))
//           FROM Attendance a
//           JOIN a.member m
//           WHERE (:typePeople IS NULL OR m.type IN :typePeople)
//             AND (:categories IS NULL OR m.category IN :categories)
//             AND (:serviceIds IS NULL OR a.services.id IN :serviceIds)
//             AND (:startDate IS NULL OR a.id.attendanceDate >= :startDate)
//             AND (:endDate IS NULL OR a.id.attendanceDate <= :endDate)
//             AND (:userId IS NULL OR m.id = :userId)
//           GROUP BY m.category
//           """)
//    List<ReportSummaryDto> countByCategory(
//            @Param("typePeople") List<String> typePeople,
//            @Param("categories") List<String> categories,
//            @Param("serviceIds") List<String> serviceIds,
//            @Param("startDate") LocalDateTime startDate,
//            @Param("endDate") LocalDateTime endDate,
//            @Param("userId") String userId
//    );

    boolean existsByMember(Member member);

    boolean existsByServices(Services service);

}
