package FinanceManangementSystem.demo.Payloads.RequestDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateUserDTO {

    @NotBlank(message = "Owner name is required")
    @Size(
            min = 2,
            max = 100,
            message = "Owner name must be between 2 and 100 characters"
    )
    @Pattern(
            regexp = "^[A-Za-z]+(?: [A-Za-z]+)*$",
            message = "Owner name must contain only letters and single spaces"
    )
    private String ownerName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    @Size(max = 100, message = "Email cannot exceed 100 characters.")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Mobile number must be a valid 10 digit Indian mobile number"
    )
    private String mobileNumber;

    @Valid
    @NotNull(message = "User address is required.")
    private RequestUserAddressDTO userAddress;

    private String currentPassword;

    @Pattern(
            regexp = "^$|^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
            message = "Password must be 8-20 characters with 1 uppercase, 1 lowercase, 1 digit, and 1 special character (@#$%^&+=!)"
    )
    private String newPassword;
}
