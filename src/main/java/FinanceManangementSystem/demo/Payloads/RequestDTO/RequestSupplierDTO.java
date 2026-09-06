package FinanceManangementSystem.demo.Payloads.RequestDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class RequestSupplierDTO {

    @NotBlank(message = "Supplier name is required")
    @Size(
            min = 2,
            max = 150,
            message = "Supplier name must be between 2 and 150 characters"
    )
    private String supplierName;


    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Mobile number must be a valid 10-digit Indian mobile number"
    )
    private String mobileNumber;


    @Size(
            max = 100,
            message = "Contact person cannot exceed 100 characters"
    )
    private String contactPerson;


    @Pattern(
            regexp = "^$|^[6-9][0-9]{9}$",
            message = "Alternate mobile number must be a valid 10-digit Indian mobile number"
    )
    private String alternateMobileNumber;


    @Email(message = "Invalid email format")
    @Size(
            max = 150,
            message = "Email cannot exceed 150 characters"
    )
    private String email;


    @Pattern(
            regexp = "^$|^[0-9A-Za-z]{15}$",
            message = "Invalid GST number"
    )
    private String gstNumber;


    @DecimalMin(
            value = "0.00",
            message = "Opening balance cannot be negative"
    )
    private BigDecimal openingBalance = BigDecimal.ZERO;


    @Min(
            value = 0,
            message = "Payment terms cannot be negative"
    )
    private Integer paymentTerms = 30;


    @Valid
    private RequestSupplierAddressDTO address;
}