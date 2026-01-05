package org.icc.pecesatierra.exceptions;

public class ServiceDesactivatedException extends RuntimeException {
    public ServiceDesactivatedException(String message) {
        super(message);
    }
}
