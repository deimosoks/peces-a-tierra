package org.icc.pecesatierra.exceptions.attendances;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotRegisterAttendanceWithDeactivatedServiceException extends ApiException {
    public CannotRegisterAttendanceWithDeactivatedServiceException(String name) {
        super("El servicio " + name + " se encuentra desactivado asi que no puede registrar asistencias con el.", HttpStatus.CONFLICT);
    }
}
