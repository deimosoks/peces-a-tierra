package org.icc.pecesatierra.exceptions.members.types;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class TypeNotFoundException extends ApiException {
    public TypeNotFoundException() {
        super("Tipo no encontrado.", HttpStatus.NOT_FOUND);
    }
}
