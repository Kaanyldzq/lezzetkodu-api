package com.lezzetkodu.api.user;

import com.lezzetkodu.api.common.NotFoundException;
import com.lezzetkodu.api.common.PageResponse;
import com.lezzetkodu.api.recipe.RecipeDtos.RecipeSummaryResponse;
import com.lezzetkodu.api.recipe.RecipeService;
import com.lezzetkodu.api.security.AuthUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Giris yapmis kullanicinin kendi bilgileri: /api/me */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserRepository userRepository;
    private final RecipeService recipeService;

    public MeController(UserRepository userRepository, RecipeService recipeService) {
        this.userRepository = userRepository;
        this.recipeService = recipeService;
    }

    public record ProfileResponse(Long id, String username, String email, Role role, Instant createdAt) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ProfileResponse profile(@AuthenticationPrincipal AuthUser me) {
        User user = userRepository.findById(me.id())
                .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı"));
        return new ProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.getCreatedAt());
    }

    @GetMapping("/favorites")
    public PageResponse<RecipeSummaryResponse> favorites(@AuthenticationPrincipal AuthUser me,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return recipeService.favoritesOf(me, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
    }
}
