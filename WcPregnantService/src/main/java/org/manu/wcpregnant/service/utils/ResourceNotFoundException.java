package org.manu.wcpregnant.service.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException { // Changez Exception en RuntimeException
    public ResourceNotFoundException(String message) {
        super(message);
    }
}