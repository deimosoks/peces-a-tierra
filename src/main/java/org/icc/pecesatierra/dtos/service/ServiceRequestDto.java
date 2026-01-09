package org.icc.pecesatierra.dtos.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

//    @NotNull
//    private DayOfWeek dayOfWeek;

//    @NotNull
//    private LocalTime startTime;

}
