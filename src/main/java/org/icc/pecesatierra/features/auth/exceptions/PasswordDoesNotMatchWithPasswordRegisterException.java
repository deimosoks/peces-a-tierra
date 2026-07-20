package org.icc.pecesatierra.features.auth.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class PasswordDoesNotMatchWithPasswordRegisterException extends ApiException {
    public PasswordDoesNotMatchWithPasswordRegisterException() {
        super("La contraseña anterior no coincide con su contraseña registrada.", HttpStatus.BAD_REQUEST);
    }
}
