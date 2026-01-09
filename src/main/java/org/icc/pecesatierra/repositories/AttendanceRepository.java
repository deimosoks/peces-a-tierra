package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.Attendance;
import org.icc.pecesatierra.entities.AttendanceId;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, AttendanceId> {

    @Query("""
        SELECT COUNT(a)
        FROM Attendance a
        WHERE a.serviceStartDate = (
            SELECT MAX(a2.serviceStartDate)
            FROM Attendance a2
        )
    """)
    long countAttendanceLastService();


    boolean existsByMember(Member member);

    boolean existsByServices(Services service);

}
