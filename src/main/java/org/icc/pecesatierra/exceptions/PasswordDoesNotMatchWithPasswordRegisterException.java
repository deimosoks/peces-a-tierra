package org.icc.pecesatierra.exceptions;

public class PasswordDoesNotMatchWithPasswordRegisterException extends RuntimeException {
    public PasswordDoesNotMatchWithPasswordRegisterException(String message) {
        super(message);
    }
}
