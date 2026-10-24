package org.icc.pecesatierra.features.role.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotManageRoleWithMorePrivilegesException extends ApiException {
    public CannotManageRoleWithMorePrivilegesException() {
        super("No puede gestionar un rol con más privilegios que los suyos.", HttpStatus.FORBIDDEN);
    }
}
