package org.icc.pecesatierra.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "discipulado")
public class Discipulado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @JoinColumn(name = "member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @Column(nullable = false, name = "date_started")
    private OffsetDateTime dateStarted;

    @JoinColumn(name = "registered_by_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Member registeredBy;

    @Column(nullable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private boolean completed;

    private OffsetDateTime dateCompleted;

    @OneToMany(mappedBy = "discipulado", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<DiscipuladoProgress> progress = new HashSet<>();

}
