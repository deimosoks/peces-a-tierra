package org.icc.pecesatierra.features.service.service.events;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class StartDateCannotBeAfterTheEndDateException extends ApiException {
    public StartDateCannotBeAfterTheEndDateException() {
        super("La fecha de inicio no puede ser después de la fecha de finalización.", HttpStatus.CONFLICT);
    }
}
