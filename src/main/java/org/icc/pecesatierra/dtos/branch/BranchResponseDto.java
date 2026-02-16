package org.icc.pecesatierra.dtos.branch;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponseDto {

    private String id;
    private String name;
    private String address;
    private String city;
    private LocalDateTime createdAt;
    private String cellphone;

}
