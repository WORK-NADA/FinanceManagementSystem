package FinanceManangementSystem.demo.Security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
                                    @org.springframework.lang.NonNull HttpServletResponse response,
                                    @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("JwtFilter - JWT Filter invoked for {} {}", request.getMethod(), request.getRequestURI());

        try{
            String authHeader = request.getHeader("Authorization");

            String token = null;
            String email = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {

                log.debug("JwtFilter - Authorization header found.");

                token = authHeader.substring(7);

                email = jwtUtil.extractEmail(token);

                log.debug("JwtFilter - JWT belongs to user: {}", email);

            } else {

                log.debug("JwtFilter - Authorization header missing or Bearer token not found.");
            }

            if (email != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                log.debug("JwtFilter - Loading user details for email: {}", email);

                try {
                    UserDetails userDetails =
                            userDetailsService.loadUserByUsername(email);

                    if (jwtUtil.validateToken(token, userDetails) && userDetails.isEnabled() && userDetails.isAccountNonLocked()) {

                        log.info("JwtFilter - JWT validated successfully for user: {}", email);

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        SecurityContextHolder.getContext()
                                .setAuthentication(authToken);

                        log.debug("JwtFilter - Security context updated for user: {}", email);

                    } else {

                        log.warn("JwtFilter - JWT validation failed for user: {}", email);
                    }
                } catch (Exception e) {
                    log.warn("JwtFilter - Could not authenticate user with token: {}", e.getMessage());
                }
            }

            filterChain.doFilter(request, response);

            log.debug("JwtFilter - Request processing completed for {} {}",
                    request.getMethod(),
                    request.getRequestURI());
        }
        catch (ExpiredJwtException e) {

            log.warn(
                    "JwtFilter - Access token expired: {}",
                    e.getMessage()
            );

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write("""
                    {
                        "code": "ACCESS_TOKEN_EXPIRED",
                        "message": "Access token expired"
                    }
                    """);

        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {

            log.warn(
                    "JwtFilter - Invalid token: {}",
                    e.getMessage()
            );

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write("""
                    {
                        "code": "INVALID_TOKEN",
                        "message": "Access token is invalid or malformed"
                    }
                    """);

        }
    }
}