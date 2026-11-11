package com.lezzetkodu.api.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring'in Page nesnesini dogrudan dondurmek yerine sade ve sabit bir JSON bicimi.
 * Istemci sadece ihtiyaci olan alanlari gorur.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
