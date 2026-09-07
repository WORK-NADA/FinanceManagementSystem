package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import FinanceManangementSystem.demo.Enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseUserDTO {
    private UUID publicId;

    private String ownerName;

    private String username;

    private String email;

    private String mobileNumber;

    private UserRole role;

    private boolean enabled;

    private ResponseUserAddressDTO userAddress;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
