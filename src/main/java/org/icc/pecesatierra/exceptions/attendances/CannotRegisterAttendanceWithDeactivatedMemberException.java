package org.icc.pecesatierra.exceptions.attendances;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotRegisterAttendanceWithDeactivatedMemberException extends ApiException {
    public CannotRegisterAttendanceWithDeactivatedMemberException(String name) {
        super("El integrante " + name + " esta desactivado asi que no se le pueden registrar asistencias.", HttpStatus.CONFLICT);
    }
}
