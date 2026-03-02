package org.icc.pecesatierra.exceptions.members;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotDeleteMemberTypeInUseException extends ApiException {
    public CannotDeleteMemberTypeInUseException(String name) {
        super("El tipo " + name + " esta en uso asi que no puede ser eliminado.", HttpStatus.CONFLICT);
    }
}
