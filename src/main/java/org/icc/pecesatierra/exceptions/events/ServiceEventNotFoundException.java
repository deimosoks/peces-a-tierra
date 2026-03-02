package org.icc.pecesatierra.exceptions.events;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class ServiceEventNotFoundException extends ApiException {
    public ServiceEventNotFoundException() {
        super("Evento no encontrado.", HttpStatus.NOT_FOUND);
    }
}
