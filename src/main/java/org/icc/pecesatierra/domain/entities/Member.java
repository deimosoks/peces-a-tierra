package org.icc.pecesatierra.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "members",
       indexes = @Index(name = "idx_cc", columnList ="cc")
)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String completeName;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean active;

    private LocalDateTime updatedAt;
    private String cc;
    private String cellphone;
    private String address;
    private LocalDate birthdate;
    private String pictureProfileUrl;

    @Override
    public String toString() {
        return "Member{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", category='" + category + '\'' +
                ", completeName='" + completeName + '\'' +
                ", createdAt=" + createdAt +
                ", active=" + active +
                ", updatedAt=" + updatedAt +
                ", cc='" + cc + '\'' +
                ", cellphone='" + cellphone + '\'' +
                ", address='" + address + '\'' +
                ", birthdate=" + birthdate +
                ", pictureProfileUrl='" + pictureProfileUrl + '\'' +
                '}';
    }
}
