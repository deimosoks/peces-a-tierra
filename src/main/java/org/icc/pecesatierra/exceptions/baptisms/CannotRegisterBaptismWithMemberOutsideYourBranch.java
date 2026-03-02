package org.icc.pecesatierra.exceptions.baptisms;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotRegisterBaptismWithMemberOutsideYourBranch extends ApiException
{
    public CannotRegisterBaptismWithMemberOutsideYourBranch() {
        super("No puede registrar bautismos a integrantes fuera de su sede.", HttpStatus.CONFLICT);
    }
}
