package org.icc.pecesatierra.features.user.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class UserDeactivatedException extends ApiException {
    public UserDeactivatedException() {
        super("Este usuario esta desactivado.", HttpStatus.CONFLICT);
    }
}
