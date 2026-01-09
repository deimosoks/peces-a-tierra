package org.icc.pecesatierra.domain.reference;

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

    private String serviceId;
    private String memberId;
    private LocalDateTime attendanceDate;

}
