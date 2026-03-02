package org.icc.pecesatierra.exceptions.members;

import lombok.Getter;
import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class AlreadyExistsMemberWithCc extends ApiException {
    public AlreadyExistsMemberWithCc(String cc) {
        super("Ya existe un integrante registrado con la identificación " + cc , HttpStatus.CONFLICT);
    }
}
