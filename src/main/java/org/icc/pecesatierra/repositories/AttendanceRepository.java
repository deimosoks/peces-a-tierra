package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.domain.entities.Attendance;
import org.icc.pecesatierra.domain.entities.AttendanceId;
import org.icc.pecesatierra.domain.entities.Member;
import org.icc.pecesatierra.domain.entities.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
