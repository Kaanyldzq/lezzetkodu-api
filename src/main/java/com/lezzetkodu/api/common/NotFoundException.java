package com.lezzetkodu.api.common;

/** Istenen kayit yoksa firlatilir; GlobalExceptionHandler bunu 404'e cevirir. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
