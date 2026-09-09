package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
public class ResponseInvestmentDTO {

    private UUID publicId;
    private UUID partnerPublicId;
    private String partnerName;
    private String partnerMobile;
    private BigDecimal partnerSharePercentage;
    private LocalDate investmentDate;
    private BigDecimal amount;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
