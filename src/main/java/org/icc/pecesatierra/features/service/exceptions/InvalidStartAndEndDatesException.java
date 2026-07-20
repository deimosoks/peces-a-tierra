package org.icc.pecesatierra.features.service.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidStartAndEndDatesException extends ApiException {
    public InvalidStartAndEndDatesException() {
        super("Fecha de inicio y fecha final son necesarias para mostrar calendario.", HttpStatus.BAD_REQUEST);
    }
}
