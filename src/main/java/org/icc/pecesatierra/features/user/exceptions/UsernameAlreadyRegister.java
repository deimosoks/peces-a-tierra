package org.icc.pecesatierra.features.user.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class UsernameAlreadyRegister extends ApiException {
    public UsernameAlreadyRegister(String username) {
        super("Ya existe un usuario registrado con el username " + username, HttpStatus.CONFLICT);
    }
}
