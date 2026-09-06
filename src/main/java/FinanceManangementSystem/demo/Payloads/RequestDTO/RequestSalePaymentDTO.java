package FinanceManangementSystem.demo.Payloads.RequestDTO;

import FinanceManangementSystem.demo.Enums.PaymentMode;
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
public class RequestSalePaymentDTO {

    @NotNull(message = "Sale is required")
    private UUID salePublicId;

    @NotNull(message = "Amount received is required")
    @DecimalMin(
            value = "0.01",
            message = "Amount received must be greater than zero"
    )
    private BigDecimal amountReceived;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @Size(
            max = 100,
            message = "Reference number cannot exceed 100 characters"
    )
    private String referenceNumber;

    @Size(
            max = 500,
            message = "Remarks cannot exceed 500 characters"
    )
    private String remarks;
}
