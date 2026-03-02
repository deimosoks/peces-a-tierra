package org.icc.pecesatierra.entities;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(
        name = "category_rules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"min_age", "max_age", "gender", "category_id", "sub_category_id"})
)
public class CategoryRules {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Integer minAge;
    private Integer maxAge;

    private String gender;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private MemberCategory memberCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "sub_category_id", nullable = true)
    private MemberSubCategory memberSubCategory;

}
