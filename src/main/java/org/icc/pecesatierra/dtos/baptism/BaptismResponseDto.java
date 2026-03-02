package org.icc.pecesatierra.dtos.baptism;

import jakarta.persistence.Column;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaptismResponseDto {

    private String id;
    private String memberName;
    private LocalDate date;
    private String note;
    private LocalDateTime createdAt;
    private String registeredBy;
    private String address;

    private String invalidReason;
    private LocalDateTime invalidAt;
    private String invalidatedBy;
    private boolean invalid;
    private String branchName;

}
