package org.icc.pecesatierra.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "member_category")
    private MemberCategory memberCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "member_type")
    private MemberType memberType;

    @Column(nullable = false)
    private LocalDateTime attendanceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by_id", nullable = false)
    private Member registeredById;

    @Column(nullable = false)
    private boolean invalid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private MemberSubCategory memberSubCategory;

    private String note;

    private String invalidReason;
    private LocalDateTime invalidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invalidator_id")
    private Member invalidatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_event_id", nullable = false)
    private ServiceEvent serviceEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

}
