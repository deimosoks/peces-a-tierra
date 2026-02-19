package org.icc.pecesatierra.exceptions;

public class CannotUpdateUserWithMemberOutSideYourBranch extends RuntimeException {
    public CannotUpdateUserWithMemberOutSideYourBranch(String message) {
        super(message);
    }
}
