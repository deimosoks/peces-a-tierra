package org.icc.pecesatierra.exceptions.services;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotDeleteServiceWithRecords extends ApiException {
    public CannotDeleteServiceWithRecords(String name) {
        super("El servicio " + name + " tiene registros asociados asi que no puede ser eliminado.", HttpStatus.CONFLICT);
    }
}
