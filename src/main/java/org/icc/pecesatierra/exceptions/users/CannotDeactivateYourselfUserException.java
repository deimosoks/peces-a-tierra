package org.icc.pecesatierra.exceptions.users;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotDeactivateYourselfUserException extends ApiException {
    public CannotDeactivateYourselfUserException() {
        super("No puedes desactivar tu propio usuario.", HttpStatus.CONFLICT);
    }
}
