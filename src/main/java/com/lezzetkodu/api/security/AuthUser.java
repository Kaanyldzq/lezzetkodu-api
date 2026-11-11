package com.lezzetkodu.api.security;

import com.lezzetkodu.api.user.Role;

/**
 * Token'dan cikan "bu istegi kim yapiyor" bilgisi.
 * Controller'larda @AuthenticationPrincipal AuthUser me seklinde alinir.
 */
public record AuthUser(Long id, String username, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
