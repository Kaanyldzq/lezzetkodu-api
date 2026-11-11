package com.lezzetkodu.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Kimlik dogrulamayi kendimiz (JWT) yaptigimiz icin Spring'in otomatik "user + rastgele sifre" kurulumunu kapatiyoruz.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class LezzetKoduApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LezzetKoduApiApplication.class, args);
    }
}
