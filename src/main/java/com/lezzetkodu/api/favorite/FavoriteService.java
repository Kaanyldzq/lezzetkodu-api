package com.lezzetkodu.api.favorite;

import com.lezzetkodu.api.common.NotFoundException;
import com.lezzetkodu.api.favorite.Favorite.FavoriteId;
import com.lezzetkodu.api.recipe.RecipeRepository;
import com.lezzetkodu.api.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final RecipeRepository recipeRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, RecipeRepository recipeRepository) {
        this.favoriteRepository = favoriteRepository;
        this.recipeRepository = recipeRepository;
    }

    /**
     * Idempotent: zaten favorideyse hata vermez, ayni sonucu dondurur.
     * Mobilde butona iki kez basilmasi veya istegin tekrar gonderilmesi sorun cikarmaz.
     */
    @Transactional
    public void add(Long recipeId, AuthUser me) {
        if (!recipeRepository.existsById(recipeId)) {
            throw new NotFoundException("Tarif bulunamadı: " + recipeId);
        }
        FavoriteId id = new FavoriteId(me.id(), recipeId);
        if (!favoriteRepository.existsById(id)) {
            favoriteRepository.save(new Favorite(me.id(), recipeId));
        }
    }

    /** Idempotent: favoride degilse de 204 doner. */
    @Transactional
    public void remove(Long recipeId, AuthUser me) {
        favoriteRepository.deleteById(new FavoriteId(me.id(), recipeId));
    }
}
