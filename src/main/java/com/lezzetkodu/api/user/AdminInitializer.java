package com.lezzetkodu.api.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uygulama acilirken ADMIN_USERNAME ve ADMIN_PASSWORD verilmisse ve boyle bir kullanici yoksa admin olusturur.
 * Sifre koda veya SQL dosyasina yazilmaz, ortam degiskeninden gelir.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            @Value("${app.admin.username}") String username,
                            @Value("${app.admin.email}") String email,
                            @Value("${app.admin.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank() || userRepository.existsByUsername(username)) {
            return;
        }
        userRepository.save(new User(username, email, passwordEncoder.encode(password), Role.ADMIN));
        log.info("Admin kullanıcısı oluşturuldu: {}", username);
    }
}
