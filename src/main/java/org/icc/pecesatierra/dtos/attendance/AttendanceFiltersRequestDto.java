package org.icc.pecesatierra.dtos.attendance;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceFiltersRequestDto {

    private String serviceId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String branchId;
    private String memberId;
    private java.util.List<String> category;
    private java.util.List<String> subCategory;

}
