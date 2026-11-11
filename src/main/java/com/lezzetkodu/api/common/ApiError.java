package com.lezzetkodu.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Butun hata cevaplari ayni bicimde doner. Istemci (mobil uygulama) her hatayi tek bir modelle okuyabilir.
 * fieldErrors sadece validation hatalarinda doludur.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }
}
