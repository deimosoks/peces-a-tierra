package org.icc.pecesatierra.exceptions.branches;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotDeleteBranchWithRecords extends ApiException {
    public CannotDeleteBranchWithRecords() {
        super("No puede borrar una sede que tiene registros asociados.", HttpStatus.CONFLICT);
    }
}
