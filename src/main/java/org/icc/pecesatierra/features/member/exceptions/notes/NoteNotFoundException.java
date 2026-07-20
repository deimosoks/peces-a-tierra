package org.icc.pecesatierra.features.member.exceptions.notes;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class NoteNotFoundException extends ApiException {
    public NoteNotFoundException() {
        super("Nota no encontrada.", HttpStatus.NOT_FOUND);
    }
}
