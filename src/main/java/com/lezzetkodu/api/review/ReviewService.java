package com.lezzetkodu.api.review;

import com.lezzetkodu.api.common.ConflictException;
import com.lezzetkodu.api.common.NotFoundException;
import com.lezzetkodu.api.common.PageResponse;
import com.lezzetkodu.api.recipe.Recipe;
import com.lezzetkodu.api.recipe.RecipeRepository;
import com.lezzetkodu.api.review.ReviewDtos.ReviewRequest;
import com.lezzetkodu.api.review.ReviewDtos.ReviewResponse;
import com.lezzetkodu.api.security.AuthUser;
import com.lezzetkodu.api.user.User;
import com.lezzetkodu.api.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, RecipeRepository recipeRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> list(Long recipeId, Pageable pageable) {
        ensureRecipeExists(recipeId);
        return PageResponse.from(reviewRepository.findByRecipeId(recipeId, pageable).map(ReviewResponse::from));
    }

    @Transactional
    public ReviewResponse create(Long recipeId, ReviewRequest request, AuthUser me) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new NotFoundException("Tarif bulunamadı: " + recipeId));
        if (recipe.isOwnedBy(me.id())) {
            throw new ConflictException("Kendi tarifinize puan veremezsiniz");
        }
        if (reviewRepository.existsByRecipeIdAndUserId(recipeId, me.id())) {
            throw new ConflictException("Bu tarife zaten yorum yaptınız");
        }
        User user = userRepository.getReferenceById(me.id());
        Review saved = reviewRepository.save(new Review(recipe, user, request.rating(), request.comment()));
        return ReviewResponse.from(saved);
    }

    private void ensureRecipeExists(Long recipeId) {
        if (!recipeRepository.existsById(recipeId)) {
            throw new NotFoundException("Tarif bulunamadı: " + recipeId);
        }
    }
}
