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
public class ResponsePurchasePaymentDTO {

    private UUID publicId;

    private String paymentNumber;

    private String referenceNumber;

    private BigDecimal amountPaid;

    private LocalDate paymentDate;

    private PaymentMode paymentMode;

    private String remarks;

    private LocalDateTime createdAt;

    private PurchaseDetails purchase;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseDetails {

        private UUID publicId;

        private String purchaseNumber;

        private String supplierName;

        private LocalDate purchaseDate;

        private BigDecimal totalAmount;

        private BigDecimal paidAmount;

        private BigDecimal pendingAmount;

        private PaymentStatus paymentStatus;
    }
}
