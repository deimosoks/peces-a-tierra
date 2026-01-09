package org.icc.pecesatierra.dtos.member;

import lombok.*;
import org.icc.pecesatierra.enums.CategoryMember;
import org.icc.pecesatierra.enums.TypeMember;

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
    private TypeMember type;
    private CategoryMember category;
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
