package org.icc.pecesatierra.exceptions.members;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotCreateMembersOutsideYourBranch extends ApiException {
    public CannotCreateMembersOutsideYourBranch() {
        super("No puede crear integrantes fuera de tu sede.", HttpStatus.CONFLICT);
    }
}
