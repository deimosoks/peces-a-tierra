package org.icc.pecesatierra.features.discipulado.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class DiscipuladoNotFoundException extends ApiException {
    public DiscipuladoNotFoundException() {
        super("Discipulado no encontrada.", HttpStatus.NOT_FOUND);
    }
}
