package org.icc.pecesatierra.dtos.member;

import lombok.*;
import org.icc.pecesatierra.enums.CategoryPerson;
import org.icc.pecesatierra.enums.TypePerson;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponseDto {

    private String id;
    private String completeName;
    private TypePerson type;
    private CategoryPerson category;
    private String cellphone;
    private String address;
    private LocalDate birthdate;
    private String cc;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String pictureProfileUrl;
    private boolean active;

    @Override
    public String toString() {
        return "MemberResponseDto{" +
                "id='" + id + '\'' +
                ", completeName='" + completeName + '\'' +
                ", type=" + type +
                ", category=" + category +
                ", cellphone='" + cellphone + '\'' +
                ", address='" + address + '\'' +
                ", birthdate=" + birthdate +
                ", cc='" + cc + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", pictureProfileUrl='" + pictureProfileUrl + '\'' +
                '}';
    }
}
