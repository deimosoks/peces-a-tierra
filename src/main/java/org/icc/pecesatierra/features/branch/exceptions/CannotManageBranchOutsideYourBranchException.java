package org.icc.pecesatierra.features.branch.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotManageBranchOutsideYourBranchException extends ApiException {
    public CannotManageBranchOutsideYourBranchException() {
        super("No puede gestionar una sede fuera de tu sede.", HttpStatus.CONFLICT);
    }
}
