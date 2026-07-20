package org.icc.pecesatierra.features.role;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class RolePermissionId implements Serializable {

    private String permission;
    private String roleId;

}
