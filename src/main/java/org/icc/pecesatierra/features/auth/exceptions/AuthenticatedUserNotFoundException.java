package org.icc.pecesatierra.features.auth.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class AuthenticatedUserNotFoundException extends ApiException {
    public AuthenticatedUserNotFoundException() {
        super("No autorizado.", HttpStatus.UNAUTHORIZED);
    }
}
