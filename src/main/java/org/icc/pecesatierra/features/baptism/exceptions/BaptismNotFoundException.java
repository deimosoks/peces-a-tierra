package org.icc.pecesatierra.features.baptism.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class BaptismNotFoundException extends ApiException {
    public BaptismNotFoundException() {
        super("Bautismo no encontrad.", HttpStatus.NOT_FOUND);
    }
}
