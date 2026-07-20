package org.icc.pecesatierra.features.user.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotUpdateYourSelfException extends ApiException {
    public CannotUpdateYourSelfException() {
        super("No puedes actualizar tu propio usuario.", HttpStatus.CONFLICT);
    }
}
