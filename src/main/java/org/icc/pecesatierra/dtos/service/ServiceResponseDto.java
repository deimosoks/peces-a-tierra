package org.icc.pecesatierra.dtos.service;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponseDto {

    private String id;
    private String name;

    private LocalDateTime createdAt;
    private String description;
    private boolean active;

    //    private String dayOfWeek;
//    private LocalTime startTime;

}
