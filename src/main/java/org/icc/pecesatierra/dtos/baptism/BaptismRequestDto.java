package org.icc.pecesatierra.dtos.baptism;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaptismRequestDto {

    @NotBlank
    private String memberId;

    @NotNull
    private LocalDate date;

    private String note;

    private String address;

    private String neighborhood;
    private String city;
    private String municipality;
    private String district;
    private String postalCode;
    private String latitude;
    private String longitude;

}
