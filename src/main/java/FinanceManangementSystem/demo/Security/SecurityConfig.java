package FinanceManangementSystem.demo.Security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        log.info("SecurityConfig - Initializing BCryptPasswordEncoder bean.");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config)
            throws Exception {

        log.info("SecurityConfig - Initializing AuthenticationManager bean.");
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        log.info("SecurityConfig - Configuring Spring Security filter chain.");

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/user/login").permitAll()
                        .requestMatchers("/user/me").hasAnyAuthority("ADMIN","CLIENT")
                        .requestMatchers("/auth/refresh").permitAll()
                        .requestMatchers("/partner/**").hasAnyAuthority("ADMIN","CLIENT")
                        .requestMatchers("/profit-distribution/**").hasAnyAuthority("ADMIN","CLIENT")
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/customer/**").hasAnyAuthority("CLIENT","ADMIN")
                        .requestMatchers("/supplier/**").hasAnyAuthority("CLIENT","ADMIN")
                        .requestMatchers("/stock/**").hasAnyAuthority("CLIENT","ADMIN")
                        .requestMatchers("/report/**").hasAnyAuthority("CLIENT","ADMIN")
                        .requestMatchers("/dashboard/**").hasAnyAuthority("CLIENT","ADMIN")
                        .requestMatchers("/stock-transaction/**").hasAnyAuthority("CLIENT","ADMIN")
                        .requestMatchers("/purchase/**").hasAnyAuthority("CLIENT","ADMIN")
                        .requestMatchers("/purchase-payment/**").hasAnyAuthority("CLIENT","ADMIN")
                        .requestMatchers("/sale/**").hasAnyAuthority("CLIENT","ADMIN")
                        .requestMatchers("/sale-payment/**").hasAnyAuthority("CLIENT","ADMIN")
                        .requestMatchers("/expense/**").hasAnyAuthority("CLIENT","ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(accessDeniedHandler())
                        .authenticationEntryPoint(authenticationEntryPoint())
                );

        http.addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        log.info("SecurityConfig - Spring Security filter chain configured successfully.");

        return http.build();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {

        return (request, response, accessDeniedException) -> {

            log.warn(
                    "Access denied. URI: {}, Method: {}, User: {}",
                    request.getRequestURI(),
                    request.getMethod(),
                    request.getUserPrincipal() != null
                            ? request.getUserPrincipal().getName()
                            : "Anonymous"
            );

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");

            response.getWriter().write("""
                    {
                        "status": 403,
                        "message": "No access to view this page"
                    }
                    """);
        };
    }

    @Bean
    public org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn(
                    "Unauthenticated request. URI: {}, Method: {}",
                    request.getRequestURI(),
                    request.getMethod()
            );

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            response.getWriter().write("""
                    {
                        "status": 401,
                        "message": "Authentication required"
                    }
                    """);
        };
    }
}