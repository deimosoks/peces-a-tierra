package org.icc.pecesatierra.features.role.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class PermissionNotFoundException extends ApiException {
    public PermissionNotFoundException() {
        super("Permiso no encontrad.", HttpStatus.NOT_FOUND);
    }
}
