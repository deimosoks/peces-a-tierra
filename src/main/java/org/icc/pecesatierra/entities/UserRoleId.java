package org.icc.pecesatierra.entities;


import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class UserRoleId implements Serializable {

    private String roleId;
    private String userId;

}
