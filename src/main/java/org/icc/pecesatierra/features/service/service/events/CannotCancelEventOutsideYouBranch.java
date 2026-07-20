package org.icc.pecesatierra.features.service.service.events;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotCancelEventOutsideYouBranch extends ApiException {
    public CannotCancelEventOutsideYouBranch() {
        super("No puede cancelar eventos fuera de tu sede.", HttpStatus.CONFLICT);
    }
}
