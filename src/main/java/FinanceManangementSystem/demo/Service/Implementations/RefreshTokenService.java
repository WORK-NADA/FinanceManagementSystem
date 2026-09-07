package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Exceptions.InvalidRefreshTokenException;
import FinanceManangementSystem.demo.Exceptions.RefreshTokenExpiredException;
import FinanceManangementSystem.demo.Model.RefreshToken;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Repository.RefreshTokenRepository;
import FinanceManangementSystem.demo.Service.RefreshTokenInterface;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class RefreshTokenService implements RefreshTokenInterface {

    private final RefreshTokenRepository refreshRepo;

    @Override
    public RefreshToken createRefreshToken(User user) {
        log.info("SERVICE - request came in refresh token creation...");

        RefreshToken refreshToken =
                RefreshToken.builder()
                        .token(UUID.randomUUID().toString())
                        .user(user)
                        .expiryDate(
                                LocalDateTime.now().plusDays(7)
                        )
                        .build();

        log.info("SERVICE - refresh token created...");

        return refreshRepo.save(refreshToken);
    }

    @Override
    public RefreshToken verifyToken(RefreshToken token) {
        log.info("SERVICE - request came in verify refresh token...");

        if (token.getUser() == null || Boolean.TRUE.equals(token.getUser().getDeleted()) || !Boolean.TRUE.equals(token.getUser().getEnabled())) {
            refreshRepo.delete(token);
            throw new InvalidRefreshTokenException("Invalid Refresh Token...");
        }

        if(token.getExpiryDate()
                .isBefore(LocalDateTime.now())){


            refreshRepo.delete(token);

            throw new RefreshTokenExpiredException(
                    "Refresh Token Expired..."
            );
        }

        log.info("SERVICE - refresh token verified...");

        return token;
    }
}
