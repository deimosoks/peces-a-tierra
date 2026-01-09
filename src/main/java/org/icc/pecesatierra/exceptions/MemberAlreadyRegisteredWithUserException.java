package org.icc.pecesatierra.exceptions;

public class MemberAlreadyRegisteredWithUserException extends RuntimeException {
    public MemberAlreadyRegisteredWithUserException(String message) {
        super(message);
    }
}
