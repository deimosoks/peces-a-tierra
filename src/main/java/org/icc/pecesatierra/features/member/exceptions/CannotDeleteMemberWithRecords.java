package org.icc.pecesatierra.features.member.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotDeleteMemberWithRecords extends ApiException {
    public CannotDeleteMemberWithRecords() {
        super("No puede eliminar integrantes con registros asociados.", HttpStatus.CONFLICT);
    }
}
