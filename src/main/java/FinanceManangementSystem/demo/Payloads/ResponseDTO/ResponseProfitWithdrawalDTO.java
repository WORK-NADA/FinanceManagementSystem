package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseProfitWithdrawalDTO {

    private UUID publicId;
    private UUID partnerPublicId;
    private String partnerName;
    private BigDecimal partnerSharePercentage;
    private LocalDate withdrawalDate;
    private BigDecimal amount;
    private BigDecimal availableBeforeWithdrawal;
    private BigDecimal remainingAfterWithdrawal;
    private String paymentMethod;
    private String referenceNumber;
    private String notes;
    private LocalDateTime createdAt;
}
