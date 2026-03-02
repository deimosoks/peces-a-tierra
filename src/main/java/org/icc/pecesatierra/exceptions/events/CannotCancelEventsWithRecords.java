package org.icc.pecesatierra.exceptions.events;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotCancelEventsWithRecords extends ApiException {
    public CannotCancelEventsWithRecords() {
        super("No puede cancelar eventos con registros asociados.", HttpStatus.CONFLICT);
    }
}
