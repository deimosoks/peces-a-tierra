package org.icc.pecesatierra.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
public class AttendanceId implements Serializable {

    @Column(nullable = false)
    private String serviceId;
    @Column(nullable = false)
    private String memberId;
    @Column(nullable = false)
    private LocalDateTime serviceDate;

}
