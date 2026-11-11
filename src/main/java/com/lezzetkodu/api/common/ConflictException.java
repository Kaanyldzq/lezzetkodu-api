package com.lezzetkodu.api.common;

/** Kayit zaten varsa (ayni kullanici adi, ayni yorum vb.) firlatilir; 409'a cevrilir. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
