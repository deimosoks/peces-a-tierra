package org.icc.pecesatierra.exceptions.auth;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends ApiException {
    public InvalidRefreshTokenException() {
        super("Session token invalido.", HttpStatus.UNAUTHORIZED);
    }
}
