package com.lezzetkodu.api.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record ReviewRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 1000) String comment
    ) {
    }

    public record ReviewResponse(Long id, String username, int rating, String comment, Instant createdAt) {
        static ReviewResponse from(Review r) {
            return new ReviewResponse(r.getId(), r.getUser().getUsername(), r.getRating(),
                    r.getComment(), r.getCreatedAt());
        }
    }
}
