package com.lezzetkodu.api.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 50) String name
    ) {
    }

    public record CategoryResponse(Long id, String name) {
        static CategoryResponse from(Category category) {
            return new CategoryResponse(category.getId(), category.getName());
        }
    }
}
