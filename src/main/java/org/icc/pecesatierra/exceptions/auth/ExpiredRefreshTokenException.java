package org.icc.pecesatierra.exceptions.auth;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class ExpiredRefreshTokenException extends ApiException {
    public ExpiredRefreshTokenException() {
        super("Session token expirado.", HttpStatus.UNAUTHORIZED);
    }
}
