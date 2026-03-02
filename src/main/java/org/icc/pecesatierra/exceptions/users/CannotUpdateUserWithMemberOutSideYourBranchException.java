package org.icc.pecesatierra.exceptions.users;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotUpdateUserWithMemberOutSideYourBranchException extends ApiException {
    public CannotUpdateUserWithMemberOutSideYourBranchException() {
        super("No puede actualizar usuarios con integrantes fuera de su sede.", HttpStatus.CONFLICT);
    }
}
