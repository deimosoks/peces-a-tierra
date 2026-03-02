package org.icc.pecesatierra.exceptions.members.types;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class TypeWithNameAlreadyExistsException extends ApiException {
    public TypeWithNameAlreadyExistsException(String name) {
        super("Tipo con nombre " + name + " ya existe.", HttpStatus.CONFLICT);
    }
}
