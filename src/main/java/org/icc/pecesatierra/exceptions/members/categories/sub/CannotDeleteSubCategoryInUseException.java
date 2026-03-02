package org.icc.pecesatierra.exceptions.members.categories.sub;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CannotDeleteSubCategoryInUseException extends ApiException {
    public CannotDeleteSubCategoryInUseException(String name) {
        super("La sub-categoria " + name + " esta en uso asi que no puede ser eliminado.", HttpStatus.CONFLICT);
    }
}
