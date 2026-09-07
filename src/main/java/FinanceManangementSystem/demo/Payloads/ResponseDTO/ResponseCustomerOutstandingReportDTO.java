package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
public class ResponseCustomerOutstandingReportDTO {

    private UUID customerPublicId;
    private String customerName;
    private String mobileNumber;
    private String email;
    private String city;
    private BigDecimal totalInvoiced;
    private BigDecimal totalReceived;
    private BigDecimal outstandingAmount;
    private LocalDate lastTransactionDate;
}
