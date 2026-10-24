package org.icc.pecesatierra.features.user.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotManageUserWithMorePrivilegesException extends ApiException {
    public CannotManageUserWithMorePrivilegesException() {
        super("No puede gestionar un usuario con más privilegios que los suyos.", HttpStatus.FORBIDDEN);
    }
}
