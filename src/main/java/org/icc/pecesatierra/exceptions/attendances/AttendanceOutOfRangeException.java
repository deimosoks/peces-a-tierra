package org.icc.pecesatierra.exceptions.attendances;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class AttendanceOutOfRangeException extends ApiException {
    public AttendanceOutOfRangeException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
