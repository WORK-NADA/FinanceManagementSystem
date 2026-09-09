package FinanceManangementSystem.demo.Controller;

import FinanceManangementSystem.demo.Exceptions.InvalidRefreshTokenException;
import FinanceManangementSystem.demo.Model.RefreshToken;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestRefreshTokenDTO;
import FinanceManangementSystem.demo.Repository.RefreshTokenRepository;
import FinanceManangementSystem.demo.Security.JwtUtil;
import FinanceManangementSystem.demo.Service.Implementations.RefreshTokenService;
import FinanceManangementSystem.demo.APIResponse.APIResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("auth")
@AllArgsConstructor
public class RefreshTokenController {

        private final RefreshTokenService refreshTokenService;

        private final RefreshTokenRepository refreshTokenRepo;

        private final JwtUtil jwtUtil;


    @PostMapping("refresh")
    public ResponseEntity<APIResponse<java.util.Map<String,String>>> refresh(@jakarta.validation.Valid @RequestBody RequestRefreshTokenDTO request){
        log.info("CONTROLLER - request came in refresh token controller...");

        RefreshToken refreshToken = refreshTokenRepo.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid Refresh Token..."));

        refreshTokenService.verifyToken(refreshToken);
        log.info("CONTROLLER - refresh token in not expired...");

        String newAccessToken = jwtUtil.generateToken(refreshToken.getUser());

        log.info("CONTROLLER - new access token sent successfully...");

        return ResponseEntity.ok(
                new APIResponse<>(
                        "New access token",
                        java.util.Map.of("accessToken", newAccessToken)
                )
        );
    }

    /**
     * Logout: invalidates the server-side refresh token so it cannot be reused
     * even if it is still stored in the client's localStorage or intercepted.
     * Called by the frontend before clearing localStorage.
     */
    @PostMapping("logout")
    public ResponseEntity<APIResponse<Void>> logout(@jakarta.validation.Valid @RequestBody RequestRefreshTokenDTO request) {
        log.info("CONTROLLER - request came in logout...");

        refreshTokenRepo.findByToken(request.getRefreshToken())
                .ifPresent(token -> {
                    refreshTokenRepo.delete(token);
                    log.info("CONTROLLER - refresh token deleted successfully for user: {}",
                            token.getUser() != null ? token.getUser().getEmail() : "unknown");
                });

        // Always return 200 even if token was not found (already expired or never existed)
        return ResponseEntity.ok(new APIResponse<>("Logged out successfully", null));
    }
}
