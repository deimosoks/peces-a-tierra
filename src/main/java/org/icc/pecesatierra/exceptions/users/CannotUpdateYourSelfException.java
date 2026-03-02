package org.icc.pecesatierra.exceptions.users;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotUpdateYourSelfException extends ApiException {
    public CannotUpdateYourSelfException() {
        super("No puedes actualizar tu propio usuario.", HttpStatus.CONFLICT);
    }
}
