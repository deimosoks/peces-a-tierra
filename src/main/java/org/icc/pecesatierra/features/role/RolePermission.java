package org.icc.pecesatierra.features.role;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "role_permission")
public class RolePermission {

    // Identidad de negocio de la PK compuesta (rol + permiso). Sin esto, el
    // HashSet del rol no deduplica y dos permisos repetidos chocan al insertar.
    @EqualsAndHashCode.Include
    @EmbeddedId
    private RolePermissionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

}
