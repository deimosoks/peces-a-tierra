package org.icc.pecesatierra.exceptions.auth;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotLogOutWithTokenYouDoNotOwn extends ApiException {
    public CannotLogOutWithTokenYouDoNotOwn() {
        super("No puedes des loguearte con un token del que no eres dueño.", HttpStatus.CONFLICT );
    }
}
