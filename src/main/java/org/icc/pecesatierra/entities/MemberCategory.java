package org.icc.pecesatierra.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "member_categories")
public class MemberCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, length = 7)
    private String color;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "category", orphanRemoval = true, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<MemberSubCategory> subCategories = new HashSet<>();

}
