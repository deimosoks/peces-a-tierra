package org.icc.pecesatierra.features.user.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotDeleteUserWithMemberOutsideYouBranch extends ApiException {
    public CannotDeleteUserWithMemberOutsideYouBranch() {
        super("No puede eliminar usuarios con integrantes fuera de su sede.", HttpStatus.CONFLICT);
    }
}
