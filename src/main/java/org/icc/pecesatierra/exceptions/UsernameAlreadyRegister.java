package org.icc.pecesatierra.exceptions;

public class UsernameAlreadyRegister extends RuntimeException {
    public UsernameAlreadyRegister(String message) {
        super(message);
    }
}
