package org.icc.pecesatierra.exceptions.members.categories.rules;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class SubcategoryDoesNotBelongToCategoryException extends ApiException {
    public SubcategoryDoesNotBelongToCategoryException(String subCategory, String category) {
        super("La sub categoria " + subCategory + " no pertenece a la categoria " + category, HttpStatus.CONFLICT);
    }
}
