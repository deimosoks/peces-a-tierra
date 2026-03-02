package org.icc.pecesatierra.dtos.user;

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
