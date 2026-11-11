package com.lezzetkodu.api.favorite;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;

/**
 * Favori = (kullanici, tarif) cifti. Ayri bir id yerine bu cift birlesik anahtar:
 * ayni tarif ayni kullanici tarafindan iki kez favorilenemez.
 */
@Entity
@Table(name = "favorites")
public class Favorite {

    @EmbeddedId
    private FavoriteId id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Favorite() {
    }

    public Favorite(Long userId, Long recipeId) {
        this.id = new FavoriteId(userId, recipeId);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public FavoriteId getId() {
        return id;
    }

    @Embeddable
    public record FavoriteId(
            @Column(name = "user_id") Long userId,
            @Column(name = "recipe_id") Long recipeId
    ) implements Serializable {
    }
}
