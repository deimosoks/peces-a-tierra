package org.icc.pecesatierra.features.discipulado.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotUpdateProgressWithMemberOutsideYourBranch extends ApiException {
    public CannotUpdateProgressWithMemberOutsideYourBranch() {
        super("No puedes actualizar el progreso de un discipulado con un integrante fuera de tu rama sin permisos de administrador.", HttpStatus.CONFLICT);
    }
}
