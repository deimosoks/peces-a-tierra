package org.icc.pecesatierra.features.member.exceptions.notes;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotManageNotesOutsideYourBranchException extends ApiException {
    public CannotManageNotesOutsideYourBranchException() {
        super("No puede gestionar notas de integrantes fuera de su sede.", HttpStatus.CONFLICT);
    }
}
