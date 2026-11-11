package com.lezzetkodu.api.recipe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

    // Liste sorgusunda kategori ve yazari ayni sorguda getir; yoksa her tarif icin 2 ek sorgu atilir (N+1 problemi).
    @Override
    @EntityGraph(attributePaths = {"category", "author"})
    Page<Recipe> findAll(Specification<Recipe> spec, Pageable pageable);

    // Detay sayfasi: tarif + kategori + yazar + malzemeler tek seferde.
    @Query("""
            select distinct r from Recipe r
            join fetch r.category
            join fetch r.author
            left join fetch r.ingredients
            where r.id = :id
            """)
    Optional<Recipe> findDetailedById(@Param("id") Long id);

    // Kullanicinin favorileri, en son favorilenen en ustte.
    @Query(value = """
            select r from Favorite f, Recipe r
            join fetch r.category
            join fetch r.author
            where f.id.recipeId = r.id and f.id.userId = :userId
            order by f.createdAt desc
            """,
            countQuery = "select count(f) from Favorite f where f.id.userId = :userId")
    Page<Recipe> findFavoritesOf(@Param("userId") Long userId, Pageable pageable);
}
