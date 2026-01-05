package org.icc.pecesatierra.dtos.report;

import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReportRowDto {

    private LocalDate date;
    private LocalTime serviceTime;

    private String serviceName;
//    private String serverDay;

    private String category;
    private String typePeople;

    private Long total;
}



