package org.icc.pecesatierra.exceptions.members;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class MemberAlreadyHasRegisteredBaptismActiveException extends ApiException {
    public MemberAlreadyHasRegisteredBaptismActiveException(String name) {
        super("El integrante " + name + " ya tiene un bautismo valido registrado.", HttpStatus.CONFLICT);
    }
}
