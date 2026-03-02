package org.icc.pecesatierra.exceptions.members;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class MemberAlreadyRegisteredWithUserException extends ApiException {
    public MemberAlreadyRegisteredWithUserException() {
        super("Este integrante ya esta enlazado a un usuario del sistema.", HttpStatus.CONFLICT);
    }
}
