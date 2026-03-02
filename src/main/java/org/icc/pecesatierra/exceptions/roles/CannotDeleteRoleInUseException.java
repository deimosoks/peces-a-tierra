package org.icc.pecesatierra.exceptions.roles;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotDeleteRoleInUseException extends ApiException {
    public CannotDeleteRoleInUseException(String name) {
        super("El rol " + name + " se encuentra en uso asi que no puede ser eliminado.", HttpStatus.CONFLICT);
    }
}
