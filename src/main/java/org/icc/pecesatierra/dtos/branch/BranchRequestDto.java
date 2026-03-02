package org.icc.pecesatierra.dtos.branch;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    private String address;

    @NotBlank
    private String city;

    @NotBlank
    private String cellphone;

}
