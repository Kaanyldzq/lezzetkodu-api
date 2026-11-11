package com.lezzetkodu.api.recipe;

import com.lezzetkodu.api.common.PageResponse;
import com.lezzetkodu.api.favorite.FavoriteService;
import com.lezzetkodu.api.recipe.RecipeDtos.RecipeDetailResponse;
import com.lezzetkodu.api.recipe.RecipeDtos.RecipeRequest;
import com.lezzetkodu.api.recipe.RecipeDtos.RecipeSummaryResponse;
import com.lezzetkodu.api.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    static final int MAX_PAGE_SIZE = 50;

    private final RecipeService recipeService;
    private final FavoriteService favoriteService;

    public RecipeController(RecipeService recipeService, FavoriteService favoriteService) {
        this.recipeService = recipeService;
        this.favoriteService = favoriteService;
    }

    /** Ornek: GET /api/recipes?categoryId=2&difficulty=EASY&q=mercimek&page=0&size=20 */
    @GetMapping
    public PageResponse<RecipeSummaryResponse> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return recipeService.search(categoryId, difficulty, q, authorId, pageOf(page, size));
    }

    @GetMapping("/{id}")
    public RecipeDetailResponse get(@PathVariable Long id) {
        return recipeService.getById(id);
    }

    // 201 Created + Location basligi: yeni kaydin adresini istemciye bildirir.
    @PostMapping
    public ResponseEntity<RecipeDetailResponse> create(@Valid @RequestBody RecipeRequest request,
                                                       @AuthenticationPrincipal AuthUser me) {
        RecipeDetailResponse created = recipeService.create(request, me);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public RecipeDetailResponse update(@PathVariable Long id, @Valid @RequestBody RecipeRequest request,
                                       @AuthenticationPrincipal AuthUser me) {
        return recipeService.update(id, request, me);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        recipeService.delete(id, me);
    }

    // Favori ekle/cikar: PUT ve DELETE ikisi de idempotent; ayni istegi tekrar gondermek sonucu degistirmez.
    @PutMapping("/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addFavorite(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        favoriteService.add(id, me);
    }

    @DeleteMapping("/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        favoriteService.remove(id, me);
    }

    /** Istemci cok buyuk sayfa isteyip sunucuyu yoramasin diye ust sinir. Yeni tarifler once gelir. */
    static PageRequest pageOf(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
