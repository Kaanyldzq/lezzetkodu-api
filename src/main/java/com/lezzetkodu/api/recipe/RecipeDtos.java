package com.lezzetkodu.api.recipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Disariya entity degil DTO donuyoruz:
 * - Sifre hash'i gibi alanlar yanlislikla JSON'a cikmaz,
 * - Veritabani yapisi degisse bile API sozlesmesi sabit kalir,
 * - Lazy iliskiler serilestirilirken hata/gereksiz sorgu olusmaz.
 */
public final class RecipeDtos {

    private RecipeDtos() {
    }

    // ---------- Istek (request) ----------

    public record IngredientRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 50) String quantity
    ) {
    }

    public record RecipeRequest(
            @NotBlank @Size(max = 120) String title,
            @Size(max = 1000) String description,
            @NotBlank String instructions,
            @NotNull @Min(1) @Max(1440) Integer prepMinutes,
            @NotNull @Min(1) @Max(100) Integer servings,
            @NotNull Difficulty difficulty,
            @NotNull Long categoryId,
            @NotEmpty @Size(max = 50) List<@Valid IngredientRequest> ingredients
    ) {
    }

    // ---------- Cevap (response) ----------

    public record IngredientResponse(String name, String quantity) {
    }

    /** Listede gosterilen kisa ozet. */
    public record RecipeSummaryResponse(
            Long id,
            String title,
            int prepMinutes,
            Difficulty difficulty,
            String categoryName,
            String authorUsername
    ) {
    }

    /** Detay sayfasi: malzemeler ve puan ortalamasi dahil. */
    public record RecipeDetailResponse(
            Long id,
            String title,
            String description,
            String instructions,
            int prepMinutes,
            int servings,
            Difficulty difficulty,
            Long categoryId,
            String categoryName,
            Long authorId,
            String authorUsername,
            List<IngredientResponse> ingredients,
            Double averageRating,
            long reviewCount,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
