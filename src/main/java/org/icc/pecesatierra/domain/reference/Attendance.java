package org.icc.pecesatierra.domain.reference;

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
    private LocalDateTime serviceStartDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("serviceId")
    @JoinColumn(name = "service_id", nullable = false)
    private Services services;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("memberId")
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Override
    public String toString() {
        return "Attendance{" +
                "id=" + id +
                ", memberCategory='" + memberCategory + '\'' +
                ", memberType='" + memberType + '\'' +
                ", serviceStartDate=" + serviceStartDate +
                ", services=" + services +
                ", member=" + member +
                '}';
    }
}
