package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, String>, JpaSpecificationExecutor<Attendance> {

    boolean existsByMember(Member member);

    boolean existsAttendanceByBranch(Branch branch);

    boolean existsByServiceEvent(ServiceEvent serviceEvent);

    @Query("SELECT a.member.id FROM Attendance a WHERE a.serviceEvent.id = :eventId AND a.invalid = false")
    Set<String> findMemberIdsByServiceEventIdInvalidFalse(String eventId);
}
