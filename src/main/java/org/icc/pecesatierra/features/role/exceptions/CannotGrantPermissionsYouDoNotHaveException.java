package org.icc.pecesatierra.features.role.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotGrantPermissionsYouDoNotHaveException extends ApiException {
    public CannotGrantPermissionsYouDoNotHaveException() {
        super("No puede otorgar permisos que usted no tiene.", HttpStatus.FORBIDDEN);
    }
}
