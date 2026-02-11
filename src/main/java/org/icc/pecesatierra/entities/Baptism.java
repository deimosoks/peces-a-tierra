package org.icc.pecesatierra.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;

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
    private LocalDateTime invalidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invalidator_id")
    private Member invalidatorId;

    @Column(nullable = false)
    private boolean invalid;

}
