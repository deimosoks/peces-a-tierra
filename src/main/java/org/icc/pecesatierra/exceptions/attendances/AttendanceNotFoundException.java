package org.icc.pecesatierra.exceptions.attendances;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class AttendanceNotFoundException extends ApiException {
    public AttendanceNotFoundException() {
        super("Asistencia no encontrada.", HttpStatus.NOT_FOUND);
    }
}
