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
public class ResponseSupplierOutstandingReportDTO {

    private UUID supplierPublicId;
    private String supplierName;
    private String mobileNumber;
    private String email;
    private String city;
    private BigDecimal totalBilled;
    private BigDecimal totalPaid;
    private BigDecimal outstandingAmount;
    private LocalDate lastTransactionDate;
}
