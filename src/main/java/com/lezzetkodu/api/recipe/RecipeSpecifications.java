package com.lezzetkodu.api.recipe;

import org.springframework.data.jpa.domain.Specification;

/**
 * Listeleme filtreleri. Her filtre sadece parametre verildiyse sorguya eklenir;
 * boylece "kategori + arama", "sadece arama", "hicbiri" gibi tum kombinasyonlar tek metotla calisir.
 */
final class RecipeSpecifications {

    private RecipeSpecifications() {
    }

    static Specification<Recipe> hasCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null
                ? null
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    static Specification<Recipe> hasDifficulty(Difficulty difficulty) {
        return (root, query, cb) -> difficulty == null
                ? null
                : cb.equal(root.get("difficulty"), difficulty);
    }

    static Specification<Recipe> titleContains(String q) {
        return (root, query, cb) -> (q == null || q.isBlank())
                ? null
                : cb.like(cb.lower(root.get("title")), "%" + q.trim().toLowerCase() + "%");
    }

    static Specification<Recipe> hasAuthor(Long authorId) {
        return (root, query, cb) -> authorId == null
                ? null
                : cb.equal(root.get("author").get("id"), authorId);
    }
}
