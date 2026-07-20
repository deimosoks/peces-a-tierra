package org.icc.pecesatierra.features.discipulado.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotFindDiscipuladoWithMemberOutsideYourBranch extends ApiException {
    public CannotFindDiscipuladoWithMemberOutsideYourBranch() {
        super("No puedes buscar un discipulado con un integrante fuera de tu rama sin permisos de administrador.", HttpStatus.CONFLICT);
    }
}
