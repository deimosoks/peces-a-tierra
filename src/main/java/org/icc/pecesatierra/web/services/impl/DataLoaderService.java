package org.icc.pecesatierra.web.services.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.icc.pecesatierra.domain.entities.Attendance;
import org.icc.pecesatierra.domain.entities.AttendanceId;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.ServiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;

@Service
@AllArgsConstructor
@Builder
public class DataLoaderService implements Runnable {

    private final ServiceRepository serviceRepository;
    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    public void run() {
        LocalDateTime from = LocalDateTime.of(2021, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2025, 12, 31, 23, 59);

        serviceRepository.findAll().forEach(service -> {
            LocalDateTime attendanceDate = randomDateTime(from, to);

            memberRepository.findAll().forEach(member -> {
                Attendance attendance = Attendance.builder()
                        .id(AttendanceId.builder()
                                .attendanceDate(LocalDateTime.now())
                                .serviceId(service.getId())
                                .memberId(member.getId())
                                .build()
                        )
                        .serviceStartDate(attendanceDate)
                        .services(service)
                        .member(member)
                        .memberType(member.getType())
                        .memberCategory(member.getCategory())
                        .build();

                attendanceRepository.save(attendance);
            });
        });
    }

    public LocalDateTime randomDateTime(
            LocalDateTime start,
            LocalDateTime end
    ) {
        long startSeconds = start.toEpochSecond(ZoneOffset.UTC);
        long endSeconds = end.toEpochSecond(ZoneOffset.UTC);

        long randomSeconds = ThreadLocalRandom
                .current()
                .nextLong(startSeconds, endSeconds);

        return LocalDateTime.ofEpochSecond(
                randomSeconds,
                0,
                ZoneOffset.UTC
        );
    }

}
