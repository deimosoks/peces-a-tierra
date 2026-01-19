package org.icc.pecesatierra.dtos.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.icc.pecesatierra.enums.CategoryMember;
import org.icc.pecesatierra.enums.TypeMember;
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
    private TypeMember type;

    @NotNull(message = "Tipo de categoria invalida.")
    private CategoryMember category;

    private String cellphone;

    private LocalDate birthdate;

    private String address;

    private String cc;

    private MultipartFile pictureProfile;

}
