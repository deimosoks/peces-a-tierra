package org.icc.pecesatierra.features.user.dtos;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserReportResponseDto {
    private long totalUsers;
    private long totalUsersActives;
}
