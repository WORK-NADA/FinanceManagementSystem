package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import FinanceManangementSystem.demo.Enums.PaymentMode;
import FinanceManangementSystem.demo.Enums.PaymentStatus;
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
public class ResponseSalePaymentDTO {

    private UUID publicId;

    private String paymentNumber;

    private String referenceNumber;

    private BigDecimal amountReceived;

    private LocalDate paymentDate;

    private PaymentMode paymentMode;

    private String remarks;

    private LocalDateTime createdAt;

    private SaleDetails sale;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleDetails {

        private UUID publicId;

        private String saleNumber;

        private String customerName;

        private LocalDate saleDate;

        private BigDecimal totalAmount;

        private BigDecimal receivedAmount;

        private BigDecimal pendingAmount;

        private PaymentStatus paymentStatus;
    }
}
