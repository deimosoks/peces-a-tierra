package org.icc.pecesatierra.exceptions;

public class ServiceDeactivatedException extends RuntimeException {
    public ServiceDeactivatedException(String message) {
        super(message);
    }
}
