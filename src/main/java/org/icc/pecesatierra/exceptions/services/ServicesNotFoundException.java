package org.icc.pecesatierra.exceptions.services;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class ServicesNotFoundException extends ApiException {
    public ServicesNotFoundException() {
        super("Servicio no encontrado.", HttpStatus.NOT_FOUND);
    }
}
