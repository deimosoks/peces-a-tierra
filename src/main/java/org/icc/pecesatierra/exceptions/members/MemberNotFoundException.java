package org.icc.pecesatierra.exceptions.members;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class MemberNotFoundException extends ApiException {
    public MemberNotFoundException() {
        super("Integrante no encontrad.", HttpStatus.NOT_FOUND);
    }
}
