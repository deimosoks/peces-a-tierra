package org.icc.pecesatierra.features.member.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class MemberNotFoundException extends ApiException {
    public MemberNotFoundException() {
        super("Integrante no encontrad.", HttpStatus.NOT_FOUND);
    }
}
