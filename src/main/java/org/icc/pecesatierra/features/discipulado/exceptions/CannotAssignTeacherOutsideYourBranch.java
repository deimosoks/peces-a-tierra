package org.icc.pecesatierra.features.discipulado.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotAssignTeacherOutsideYourBranch extends ApiException {
    public CannotAssignTeacherOutsideYourBranch() {
        super("No puedes asignar un maestro fuera de tu rama sin permisos de administrador.", HttpStatus.CONFLICT);
    }
}
