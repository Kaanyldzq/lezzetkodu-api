package com.lezzetkodu.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI'da "Authorize" butonu cikar; token'i bir kez girip korumali endpoint'leri deneyebilirsin. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI lezzetKoduOpenApi() {
        String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info().title("LezzetKodu API").version("1.0.0")
                        .description("Tarif, kategori, favori ve yorum işlemleri için REST API"))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
