package FinanceManangementSystem.demo.Payloads.RequestDTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class RequestOpeningBalanceDTO {

    @NotNull(message = "Opening balance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Opening balance must be greater than or equal to 0")
    private BigDecimal openingBalance;

    public RequestOpeningBalanceDTO(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }
}
