package org.icc.pecesatierra.exceptions;

public class MemberNoHasCategoryForThisSubCategoryException extends RuntimeException {
    public MemberNoHasCategoryForThisSubCategoryException(String message) {
        super(message);
    }
}
