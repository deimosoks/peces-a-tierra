package org.icc.pecesatierra.dtos.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.icc.pecesatierra.enums.CategoryPerson;
import org.icc.pecesatierra.enums.TypePerson;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberRequestDto {

    @NotBlank
    private String completeName;

    @NotNull
    private TypePerson type;

    @NotNull
    private CategoryPerson category;

    private String cellphone;

    private LocalDate birthdate;

    private String address;

    private String cc;

    private MultipartFile pictureProfile;

    @Override
    public String toString() {
        return "MemberRequestDto{" +
                "completeName='" + completeName + '\'' +
                ", type=" + type +
                ", category=" + category +
                ", cellphone='" + cellphone + '\'' +
                ", birthdate=" + birthdate +
                ", address='" + address + '\'' +
                ", cc='" + cc + '\'' +
                ", pictureProfile=" + pictureProfile +
                '}';
    }
}
