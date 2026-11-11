package com.lezzetkodu.api.review;

import com.lezzetkodu.api.common.PageResponse;
import com.lezzetkodu.api.review.ReviewDtos.ReviewRequest;
import com.lezzetkodu.api.review.ReviewDtos.ReviewResponse;
import com.lezzetkodu.api.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes/{recipeId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public PageResponse<ReviewResponse> list(@PathVariable Long recipeId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewService.list(recipeId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@PathVariable Long recipeId, @Valid @RequestBody ReviewRequest request,
                                 @AuthenticationPrincipal AuthUser me) {
        return reviewService.create(recipeId, request, me);
    }
}
