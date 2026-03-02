package org.icc.pecesatierra.exceptions.baptisms;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotInvalidBaptismOutsideYourBranch extends ApiException {
    public CannotInvalidBaptismOutsideYourBranch() {
        super("No puede invalidar bautismos fuera de su sede.", HttpStatus.CONFLICT);
    }
}
