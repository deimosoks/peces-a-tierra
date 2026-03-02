package org.icc.pecesatierra.exceptions.branches;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class BranchNotFoundException extends ApiException {
    public BranchNotFoundException() {
        super("Sede no encontrada.", HttpStatus.NOT_FOUND);
    }
}
