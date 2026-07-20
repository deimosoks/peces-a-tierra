package org.icc.pecesatierra.features.discipulado.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotCreateDiscipuladoWithMemberOutsideYourBranch extends ApiException {
    public CannotCreateDiscipuladoWithMemberOutsideYourBranch() {
        super("No puedes registrar un discipulado con un integrante fuera de tu rama sin permisos de administrador.", HttpStatus.CONFLICT);
    }
}
