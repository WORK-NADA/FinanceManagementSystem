package FinanceManangementSystem.demo.Payloads.RequestDTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestInvestmentDTO {

    @NotNull(message = "Partner selection is required")
    private UUID partnerPublicId;

    @NotNull(message = "Investment date is required")
    private LocalDate investmentDate;

    @NotNull(message = "Amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Investment amount must be greater than 0"
    )
    private BigDecimal amount;

    @Size(
            max = 500,
            message = "Description cannot exceed 500 characters"
    )
    private String description;
}
