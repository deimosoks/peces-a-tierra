package org.icc.pecesatierra.features.member.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class FileErrorException extends ApiException {
    public FileErrorException() {
        super("Error al procesar su solicitud, por favor intente mas tarde.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
