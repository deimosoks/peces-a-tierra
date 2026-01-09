package org.icc.pecesatierra.domain.reference;

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

    private String permissionId;
    private String roleId;

}
