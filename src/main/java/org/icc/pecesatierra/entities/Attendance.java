package org.icc.pecesatierra.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "attendance")
public class Attendance {

    @EmbeddedId
    private AttendanceId id;

    @Column(nullable = false)
    private String memberCategory;

    @Column(nullable = false)
    private String memberType;

    @Column(nullable = false)
    private LocalDateTime attendanceDate;

    @Column(nullable = false)
    private boolean invalid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Member registeredById;

    private String note;

    private String invalidReason;
    private LocalDateTime invalidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invalidator_id")
    private Member invalidatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("serviceId")
    @JoinColumn(name = "service_id", nullable = false)
    private Services services;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("memberId")
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

}
