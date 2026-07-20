package org.icc.pecesatierra.features.discipulado;

import jakarta.persistence.*;
import lombok.*;
import org.icc.pecesatierra.features.member.Member;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "discipulado_progress")
public class DiscipuladoProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @JoinColumn(name = "discipulado_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Discipulado discipulado;

    @Column(nullable = false)
    private int step;

    @JoinColumn(name = "teacher_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member teacherId;

    @Column(nullable = false)
    private boolean completed;

    private OffsetDateTime dateCompleted;

    @JoinColumn(name = "registered_by_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Member registeredBy;

    @Column(nullable = false, name = "created_at")
    private OffsetDateTime createdAt;

}
