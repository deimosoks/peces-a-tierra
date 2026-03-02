package org.icc.pecesatierra.exceptions.images;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidImageFormatException extends ApiException {
    public InvalidImageFormatException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
