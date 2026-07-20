package org.icc.pecesatierra.features.user.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException() {
        super("Usuario no encontrado.", HttpStatus.NOT_FOUND);
    }
}
