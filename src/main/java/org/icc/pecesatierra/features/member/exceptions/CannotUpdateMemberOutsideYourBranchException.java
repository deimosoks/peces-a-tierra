package org.icc.pecesatierra.features.member.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotUpdateMemberOutsideYourBranchException extends ApiException {
    public CannotUpdateMemberOutsideYourBranchException() {
        super("No puede actualizar integrantes fuera de su sede.", HttpStatus.CONFLICT);
    }
}
