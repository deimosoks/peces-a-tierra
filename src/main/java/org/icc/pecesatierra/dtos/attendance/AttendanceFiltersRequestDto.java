package org.icc.pecesatierra.dtos.attendance;

import lombok.*;
import org.icc.pecesatierra.utils.enums.Gender;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceFiltersRequestDto {

    private String serviceId;
    private String eventId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String branchId;
    private String memberId;
    private List<String> category;
    private List<String> subCategory;
    private Boolean invalid;
    private String gender;

}
