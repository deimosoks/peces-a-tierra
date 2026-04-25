package org.icc.pecesatierra.exceptions.discipulado;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class DiscipuladoProgressNotFoundException extends ApiException {
    public DiscipuladoProgressNotFoundException() {
        super("Discipulado no encontrada.", HttpStatus.NOT_FOUND);
    }
}
