package org.icc.pecesatierra.exceptions;

public class CannotCreateUsersWithMembersOutsideYourBranch extends RuntimeException {
    public CannotCreateUsersWithMembersOutsideYourBranch(String message) {
        super(message);
    }
}
