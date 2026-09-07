package FinanceManangementSystem.demo.Security;

import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        log.debug("CustomUserDetailsService - Loading user details for email: {}", email);

        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";

        User user = userRepo.findByEmail(normalizedEmail)
                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
                .orElseThrow(() -> {
                    log.warn("CustomUserDetailsService - User not found or deleted with email: {}", normalizedEmail);
                    return new UsernameNotFoundException(
                            "Incorrect email or password. Please check your details and try again."
                    );
                });

        log.info("CustomUserDetailsService - User loaded successfully: {}", normalizedEmail);
        log.debug("CustomUserDetailsService - Assigned role: {}", user.getRole().name());

        boolean enabled = Boolean.TRUE.equals(user.getEnabled()) && !Boolean.TRUE.equals(user.getDeleted());
        boolean accountNonExpired = Boolean.TRUE.equals(user.getAccountNonExpired());
        boolean credentialsNonExpired = Boolean.TRUE.equals(user.getCredentialsNonExpired());
        boolean accountNonLocked = Boolean.TRUE.equals(user.getAccountNonLocked());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                enabled,
                accountNonExpired,
                credentialsNonExpired,
                accountNonLocked,
                List.of(
                        new SimpleGrantedAuthority(
                                user.getRole().name()
                        )
                )
        );
    }
}