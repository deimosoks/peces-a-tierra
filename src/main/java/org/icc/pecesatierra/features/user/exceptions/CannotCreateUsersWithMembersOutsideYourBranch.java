package org.icc.pecesatierra.features.user.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotCreateUsersWithMembersOutsideYourBranch extends ApiException {
    public CannotCreateUsersWithMembersOutsideYourBranch() {
        super("No puede crear usuarios con integrantes fuera de su sede.", HttpStatus.CONFLICT);
    }
}
