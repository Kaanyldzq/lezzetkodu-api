package com.lezzetkodu.api.recipe;

import com.lezzetkodu.api.recipe.RecipeDtos.IngredientResponse;
import com.lezzetkodu.api.recipe.RecipeDtos.RecipeDetailResponse;
import com.lezzetkodu.api.recipe.RecipeDtos.RecipeSummaryResponse;

/** Entity -> DTO donusumleri tek yerde. */
final class RecipeMapper {

    private RecipeMapper() {
    }

    static RecipeSummaryResponse toSummary(Recipe r) {
        return new RecipeSummaryResponse(
                r.getId(),
                r.getTitle(),
                r.getPrepMinutes(),
                r.getDifficulty(),
                r.getCategory().getName(),
                r.getAuthor().getUsername());
    }

    static RecipeDetailResponse toDetail(Recipe r, Double averageRating, long reviewCount) {
        return new RecipeDetailResponse(
                r.getId(),
                r.getTitle(),
                r.getDescription(),
                r.getInstructions(),
                r.getPrepMinutes(),
                r.getServings(),
                r.getDifficulty(),
                r.getCategory().getId(),
                r.getCategory().getName(),
                r.getAuthor().getId(),
                r.getAuthor().getUsername(),
                r.getIngredients().stream()
                        .map(i -> new IngredientResponse(i.getName(), i.getQuantity()))
                        .toList(),
                averageRating == null ? null : Math.round(averageRating * 10) / 10.0,
                reviewCount,
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
