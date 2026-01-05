package org.icc.pecesatierra.utils;

public class Validator {

    public static <T extends RuntimeException> void validate(boolean condition, T exceptionToThrow) {
        if (!condition) {
            throw exceptionToThrow;
        }
    }

}
