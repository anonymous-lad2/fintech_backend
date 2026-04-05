package com.fintech.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException forUser(Object id) {
        return new ResourceNotFoundException("User not found with id: " +id);
    }

    public static ResourceNotFoundException forRecord(Object id) {
        return new ResourceNotFoundException("Financial Record not found with id: " +id);
    }
}
