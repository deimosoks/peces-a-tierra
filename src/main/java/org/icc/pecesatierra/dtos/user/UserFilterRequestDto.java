package org.icc.pecesatierra.dtos.user;

import lombok.*;
import org.icc.pecesatierra.utils.models.OrderBy;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterRequestDto {

    private String query;
    private String branchId;
    private OrderBy orderBy;

}
