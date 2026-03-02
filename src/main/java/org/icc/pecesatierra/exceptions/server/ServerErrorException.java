package org.icc.pecesatierra.exceptions.server;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class ServerErrorException extends ApiException {
    public ServerErrorException() {
        super("Error al procesar su solicitud, por favor intente mas tarde.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
