package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, String> {

//    @Query("""
//                SELECT COUNT(a)
//                FROM Attendance a
//                WHERE invalid = false AND
//                a.id.serviceDate = (
//                    SELECT MAX(a2.id.serviceDate)
//                    FROM Attendance a2
//                )
//            """)
//    long countAttendanceLastService();


    boolean existsByMember(Member member);

    boolean existsByMemberAndServiceEvent(Member member, ServiceEvent event);

    boolean existsAttendanceByBranch(Branch branch);

}
