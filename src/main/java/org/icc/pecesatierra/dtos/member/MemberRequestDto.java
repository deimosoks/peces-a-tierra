package org.icc.pecesatierra.dtos.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.icc.pecesatierra.utils.enums.Gender;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberRequestDto {

    @NotBlank(message = "Nombre invalido.")
    private String completeName;

    @NotNull(message = "Tipo de integrante invalido.")
    private String typeId;

    @NotNull(message = "Tipo de categoria invalida.")
    private String categoryId;

    private boolean categoryLocked;

    private String subCategoryId;

    private String cellphone;

    private LocalDate birthdate;

    private String address;

    private String cc;

    private MultipartFile pictureProfile;
    //address
    private String neighborhood;
    private String city;
    private String municipality;
    private String district;
    private String postalCode;
    private String latitude;
    private String longitude;
    private Gender gender;
    private String branchId;

}
