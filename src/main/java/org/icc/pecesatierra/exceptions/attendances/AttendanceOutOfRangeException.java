package org.icc.pecesatierra.exceptions.attendances;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class AttendanceOutOfRangeException extends ApiException {
    public AttendanceOutOfRangeException() {
        super("No puede registrar asistencias en un evento finalizado.", HttpStatus.BAD_REQUEST);
    }
}
