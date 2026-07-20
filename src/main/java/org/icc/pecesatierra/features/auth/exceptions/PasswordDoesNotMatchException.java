package org.icc.pecesatierra.features.auth.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class PasswordDoesNotMatchException extends ApiException {
    public PasswordDoesNotMatchException() {
        super("Las contraseñas no coinciden.", HttpStatus.BAD_REQUEST);
    }
}
