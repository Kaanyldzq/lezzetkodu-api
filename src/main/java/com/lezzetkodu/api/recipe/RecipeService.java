package com.lezzetkodu.api.recipe;

import com.lezzetkodu.api.category.CategoryService;
import com.lezzetkodu.api.common.NotFoundException;
import com.lezzetkodu.api.common.PageResponse;
import com.lezzetkodu.api.recipe.Recipe.IngredientData;
import com.lezzetkodu.api.recipe.RecipeDtos.RecipeDetailResponse;
import com.lezzetkodu.api.recipe.RecipeDtos.RecipeRequest;
import com.lezzetkodu.api.recipe.RecipeDtos.RecipeSummaryResponse;
import com.lezzetkodu.api.review.ReviewRepository;
import com.lezzetkodu.api.review.ReviewRepository.RatingStats;
import com.lezzetkodu.api.security.AuthUser;
import com.lezzetkodu.api.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;

    public RecipeService(RecipeRepository recipeRepository, ReviewRepository reviewRepository,
                         UserRepository userRepository, CategoryService categoryService) {
        this.recipeRepository = recipeRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeSummaryResponse> search(Long categoryId, Difficulty difficulty, String q,
                                                       Long authorId, Pageable pageable) {
        Specification<Recipe> spec = Specification.allOf(
                RecipeSpecifications.hasCategory(categoryId),
                RecipeSpecifications.hasDifficulty(difficulty),
                RecipeSpecifications.titleContains(q),
                RecipeSpecifications.hasAuthor(authorId));
        return PageResponse.from(recipeRepository.findAll(spec, pageable).map(RecipeMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public RecipeDetailResponse getById(Long id) {
        Recipe recipe = recipeRepository.findDetailedById(id)
                .orElseThrow(() -> recipeNotFound(id));
        return toDetail(recipe);
    }

    @Transactional
    public RecipeDetailResponse create(RecipeRequest request, AuthUser me) {
        Recipe recipe = new Recipe(userRepository.getReferenceById(me.id()));
        apply(recipe, request);
        recipeRepository.save(recipe);
        return getById(recipe.getId());
    }

    @Transactional
    public RecipeDetailResponse update(Long id, RecipeRequest request, AuthUser me) {
        Recipe recipe = loadForChange(id, me);
        apply(recipe, request);
        recipeRepository.flush();
        return toDetail(recipe);
    }

    @Transactional
    public void delete(Long id, AuthUser me) {
        recipeRepository.delete(loadForChange(id, me));
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeSummaryResponse> favoritesOf(AuthUser me, Pageable pageable) {
        return PageResponse.from(recipeRepository.findFavoritesOf(me.id(), pageable).map(RecipeMapper::toSummary));
    }

    // Sadece tarifin sahibi veya admin degistirebilir/silebilir.
    private Recipe loadForChange(Long id, AuthUser me) {
        Recipe recipe = recipeRepository.findDetailedById(id).orElseThrow(() -> recipeNotFound(id));
        if (!recipe.isOwnedBy(me.id()) && !me.isAdmin()) {
            throw new AccessDeniedException("Bu tarifi değiştirme yetkiniz yok");
        }
        return recipe;
    }

    private void apply(Recipe recipe, RecipeRequest request) {
        recipe.update(
                request.title().trim(),
                request.description(),
                request.instructions(),
                request.prepMinutes(),
                request.servings(),
                request.difficulty(),
                categoryService.getEntity(request.categoryId()));
        recipe.replaceIngredients(request.ingredients().stream()
                .map(i -> new IngredientData(i.name().trim(), i.quantity().trim()))
                .toList());
    }

    private RecipeDetailResponse toDetail(Recipe recipe) {
        RatingStats stats = reviewRepository.ratingStats(recipe.getId());
        return RecipeMapper.toDetail(recipe, stats.getAverage(), stats.getReviewCount());
    }

    private static NotFoundException recipeNotFound(Long id) {
        return new NotFoundException("Tarif bulunamadı: " + id);
    }
}
