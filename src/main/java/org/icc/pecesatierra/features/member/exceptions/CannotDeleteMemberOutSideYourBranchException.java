package org.icc.pecesatierra.features.member.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotDeleteMemberOutSideYourBranchException extends ApiException {
    public CannotDeleteMemberOutSideYourBranchException() {
        super("No puede eliminar integrantes fuera de su sede.", HttpStatus.CONFLICT);
    }
}
