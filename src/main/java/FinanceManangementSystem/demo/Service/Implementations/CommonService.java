package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Exceptions.DuplicateResourceException;
import FinanceManangementSystem.demo.Exceptions.InvalidRequestException;
import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Model.UserAddress;
import FinanceManangementSystem.demo.Repository.RefreshTokenRepository;
import FinanceManangementSystem.demo.Repository.UserRepository;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestLoginDTO;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestUpdateUserDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseLoginDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseUserAddressDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseUserDTO;
import FinanceManangementSystem.demo.Security.JwtUtil;
import FinanceManangementSystem.demo.Service.CommonServiceInterface;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class CommonService implements CommonServiceInterface {

    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;

    private final UserRepository userRepo;

    private final RefreshTokenService refreshTokenService;

    private final RefreshTokenRepository refreshRepo;

    private final CurrentUserService currentUserService;

    private final ModelMapper modelMapper;

    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public ResponseLoginDTO login(RequestLoginDTO dto) {
        log.info("SERVICE - request came in login...");

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.getEmail(),
                        dto.getPassword()
                )
        );

        Optional<User> checkUser = userRepo.findByEmail(dto.getEmail());
        if(checkUser.isEmpty()){
            throw new ResourceNotFoundException("User not found...");
        }

        User user = checkUser.get();

        // Access Token
        String token =
                jwtUtil.generateToken(user);

        //delete old refresh token when not expired and login again.
        refreshRepo.deleteByUser(user.getUserId());
        log.info("SERVICE - deleted old refresh token...");

        log.info("SERVICE - logged in successfully and created new refresh token...");
        // Refresh Token
        return ResponseLoginDTO.builder()

                .accessToken(token)

                .refreshToken(
                        refreshTokenService.createRefreshToken(user).getToken()
                )

                .tokenType("Bearer")

                .publicId(
                        user.getPublicId()
                )

                .ownerName(
                        user.getOwnerName()
                )

                .userName(
                        user.getUsername()
                )

                .email(
                        user.getEmail()
                )

                .role(
                        user.getRole().name()
                )

                .firstLogin(
                        user.getFirstLogin()
                )

                .loginTime(
                        LocalDateTime.now()
                )

                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseUserDTO getCurrentUser() {
        log.info("SERVICE - request came in getCurrentUser...");
        User user = currentUserService.getCurrentUser();
        ResponseUserDTO response = modelMapper.map(user, ResponseUserDTO.class);
        if (user.getAddress() != null) {
            response.setUserAddress(modelMapper.map(user.getAddress(), ResponseUserAddressDTO.class));
        }
        log.info("SERVICE - current user fetched successfully...");
        return response;
    }

    @Transactional
    @Override
    public ResponseUserDTO updateCurrentUser(RequestUpdateUserDTO dto) {
        log.info("SERVICE - request came in updateCurrentUser...");
        User user = currentUserService.getCurrentUser();

        // Check for email or mobile conflict with other users
        Optional<String> duplicate = userRepo.findByEmailOrContactAndNotPublicId(dto.getEmail(), dto.getMobileNumber(), user.getPublicId());
        if (duplicate.isPresent()) {
            throw new DuplicateResourceException("Another user with this email or mobile number already exists.");
        }

        // Handle password update if provided
        if (dto.getNewPassword() != null && !dto.getNewPassword().trim().isEmpty()) {
            if (dto.getCurrentPassword() == null || dto.getCurrentPassword().trim().isEmpty()) {
                throw new InvalidRequestException("Current password is required to change password.");
            }
            if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                throw new InvalidRequestException("Current password is incorrect.");
            }
            user.setPassword(passwordEncoder.encode(dto.getNewPassword().trim()));
            log.info("SERVICE - password updated for user: {}", user.getUsername());
        }

        user.setOwnerName(dto.getOwnerName());
        user.setEmail(dto.getEmail());
        user.setMobileNumber(dto.getMobileNumber());

        if (dto.getUserAddress() != null) {
            UserAddress address = user.getAddress();
            if (address == null) {
                address = new UserAddress();
                address.setUser(user);
                user.setAddress(address);
            }
            address.setHouseNo(dto.getUserAddress().getHouseNo());
            address.setSocietyName(dto.getUserAddress().getSocietyName());
            address.setArea(dto.getUserAddress().getArea());
            address.setCity(dto.getUserAddress().getCity());
            address.setPincode(dto.getUserAddress().getPincode());
            address.setState(dto.getUserAddress().getState());
            address.setCountry(dto.getUserAddress().getCountry() != null && !dto.getUserAddress().getCountry().isBlank()
                    ? dto.getUserAddress().getCountry() : "India");
        }

        user = userRepo.save(user);
        log.info("SERVICE - current user updated successfully...");

        ResponseUserDTO response = modelMapper.map(user, ResponseUserDTO.class);
        if (user.getAddress() != null) {
            response.setUserAddress(modelMapper.map(user.getAddress(), ResponseUserAddressDTO.class));
        }
        return response;
    }
}
