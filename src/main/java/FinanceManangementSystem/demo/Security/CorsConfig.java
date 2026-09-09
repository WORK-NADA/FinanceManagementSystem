package FinanceManangementSystem.demo.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // Allowed frontend origins
        config.setAllowedOrigins(allowedOrigins);

        // Allowed HTTP methods
        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        // Allow all request headers
        config.setAllowedHeaders(List.of("*"));

        // Allow credentials
        config.setAllowCredentials(true);

        // Cache CORS preflight (OPTIONS) response for 1 hour to avoid an extra
        // network round-trip before every POST/PUT/DELETE request
        config.setMaxAge(3600L);

        // Expose headers the browser is allowed to read from the response
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}