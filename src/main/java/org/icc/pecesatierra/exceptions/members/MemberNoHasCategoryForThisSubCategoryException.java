package org.icc.pecesatierra.exceptions.members;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class MemberNoHasCategoryForThisSubCategoryException extends ApiException {
    public MemberNoHasCategoryForThisSubCategoryException(String memberName) {
        super("El integrante " + memberName + " no cumple los requisitos para recibir esta sub-categoria.", HttpStatus.CONFLICT);
    }
}
