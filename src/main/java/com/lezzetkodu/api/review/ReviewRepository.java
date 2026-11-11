package com.lezzetkodu.api.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "user")
    Page<Review> findByRecipeId(Long recipeId, Pageable pageable);

    boolean existsByRecipeIdAndUserId(Long recipeId, Long userId);

    /** Ortalama ve sayiyi Java'da degil veritabaninda hesapla: tum yorumlari bellege cekmeye gerek yok. */
    @Query("select avg(r.rating) as average, count(r) as reviewCount from Review r where r.recipe.id = :recipeId")
    RatingStats ratingStats(@Param("recipeId") Long recipeId);

    interface RatingStats {
        Double getAverage();

        Long getReviewCount();
    }
}
