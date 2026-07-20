package org.icc.pecesatierra.features.role.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class RoleNotFoundException extends ApiException {
    public RoleNotFoundException() {
        super("Rol no encontrado.", HttpStatus.NOT_FOUND);
    }
}
