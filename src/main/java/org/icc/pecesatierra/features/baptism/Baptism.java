package org.icc.pecesatierra.features.baptism;

import jakarta.persistence.*;
import lombok.*;
import org.icc.pecesatierra.features.member.Member;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "baptism")
public class Baptism {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @JoinColumn(name = "member_baptized", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Member baptizedMember;

    @Column(nullable = false)
    private LocalDate date;


    private String note;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @JoinColumn(name = "registered_by", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Member registeredBy;

    private String address;

    //address
    private String neighborhood;
    private String city;
    private String municipality;
    private String district;
    private String postalCode;
    private String latitude;
    private String longitude;

    //invalid
    private String invalidReason;
    private OffsetDateTime invalidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invalidator_id")
    private Member invalidatorId;

    @Column(nullable = false)
    private boolean invalid;

}
