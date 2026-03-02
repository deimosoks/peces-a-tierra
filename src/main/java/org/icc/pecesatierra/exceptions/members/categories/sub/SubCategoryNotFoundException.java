package org.icc.pecesatierra.exceptions.members.categories.sub;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class SubCategoryNotFoundException extends ApiException {
    public SubCategoryNotFoundException() {
        super("Sub-categoria no encontrada.", HttpStatus.NOT_FOUND);
    }
}
