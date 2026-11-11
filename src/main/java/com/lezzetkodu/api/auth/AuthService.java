package com.lezzetkodu.api.auth;

import com.lezzetkodu.api.auth.AuthDtos.AuthResponse;
import com.lezzetkodu.api.auth.AuthDtos.LoginRequest;
import com.lezzetkodu.api.auth.AuthDtos.RegisterRequest;
import com.lezzetkodu.api.common.ConflictException;
import com.lezzetkodu.api.security.JwtService;
import com.lezzetkodu.api.user.Role;
import com.lezzetkodu.api.user.User;
import com.lezzetkodu.api.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Bu kullanıcı adı alınmış");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Bu e-posta ile kayıtlı bir hesap var");
        }
        // Kayit olan herkes USER olur; ADMIN rolu disaridan istekle verilemez.
        User user = userRepository.save(
                new User(username, email, passwordEncoder.encode(request.password()), Role.USER));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Kullanici yok da olsa sifre yanlis da olsa ayni mesaj: hangi kullanici adlarinin var oldugu sizmaz.
        User user = userRepository.findByUsername(request.username().trim())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Kullanıcı adı veya şifre hatalı"));
        return tokenFor(user);
    }

    private AuthResponse tokenFor(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer", jwtService.expiresInSeconds(),
                user.getUsername());
    }
}
