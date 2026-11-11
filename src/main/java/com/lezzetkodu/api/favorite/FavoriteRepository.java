package com.lezzetkodu.api.favorite;

import com.lezzetkodu.api.favorite.Favorite.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {
}
