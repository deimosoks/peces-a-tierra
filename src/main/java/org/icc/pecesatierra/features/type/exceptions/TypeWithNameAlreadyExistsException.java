package org.icc.pecesatierra.features.type.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class TypeWithNameAlreadyExistsException extends ApiException {
    public TypeWithNameAlreadyExistsException(String name) {
        super("Tipo con nombre " + name + " ya existe.", HttpStatus.CONFLICT);
    }
}
