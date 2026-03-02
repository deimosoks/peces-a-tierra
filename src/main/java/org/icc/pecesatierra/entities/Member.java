package org.icc.pecesatierra.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "members", indexes = @Index(name = "idx_cc", columnList = "cc"))
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String completeName;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean active;

    private LocalDateTime updatedAt;

    private String cc;
    private String cellphone;
    private LocalDate birthdate;

    private String pictureProfileUrl;
    private String publicId;
    //address
    private String address;
    //address for search
    private String neighborhood;
    private String city;
    private String municipality;
    private String district;
    private String postalCode;
    private String latitude;
    private String longitude;
    private String gender;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private MemberType typeId;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private MemberCategory categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<MemberNotes> notes = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private MemberSubCategory subcategoryId;

    @Column(nullable = false, name = "category_locked")
    @Builder.Default
    private boolean categoryLocked = false;

}
