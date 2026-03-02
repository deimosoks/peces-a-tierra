package org.icc.pecesatierra.exceptions.permissions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class PermissionNotFoundException extends ApiException {
    public PermissionNotFoundException() {
        super("Permiso no encontrad.", HttpStatus.NOT_FOUND);
    }
}
