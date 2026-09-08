package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Client360DTO {

    // 1. Account Profile
    private UUID publicId;
    private String ownerName;
    private String username;
    private String email;
    private String mobileNumber;
    private String role;
    private Boolean enabled;
    private Boolean accountNonLocked;
    private Integer failedLoginAttempts;
    private LocalDateTime lockTime;
    private LocalDateTime createdAt;
    private ResponseUserAddressDTO address;
    private String viewablePassword;

    // 2. Financial Metrics Rollup
    private BigDecimal openingBalance = BigDecimal.ZERO;
    private BigDecimal totalGrossSales = BigDecimal.ZERO;
    private BigDecimal totalGrossPurchases = BigDecimal.ZERO;
    private BigDecimal totalPaymentsReceived = BigDecimal.ZERO;
    private BigDecimal totalPaymentsMade = BigDecimal.ZERO;
    private BigDecimal totalExpenses = BigDecimal.ZERO;
    private BigDecimal totalWithdrawals = BigDecimal.ZERO;
    private BigDecimal netProfit = BigDecimal.ZERO;
    private BigDecimal totalBalance = BigDecimal.ZERO;
    private BigDecimal totalReceivables = BigDecimal.ZERO;
    private BigDecimal totalPayables = BigDecimal.ZERO;
    private BigDecimal netWorkingCapital = BigDecimal.ZERO;

    // 3. Entity Counters
    private long totalCustomers;
    private long totalSuppliers;
    private long totalStockItems;
    private long totalPartners;
    private long totalSalesCount;
    private long totalPurchasesCount;

    // 4. Partner Structure
    private List<ClientPartnerDTO> partners;

    // 5. Recent Transaction Streams
    private List<ClientRecentSaleDTO> recentSales;
    private List<ClientRecentPurchaseDTO> recentPurchases;
    private List<ClientRecentPaymentDTO> recentPayments;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientPartnerDTO {
        private UUID publicId;
        private String partnerName;
        private String mobileNumber;
        private String email;
        private BigDecimal sharePercentage;
        private BigDecimal totalWithdrawn;
        private Boolean isActive;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientRecentSaleDTO {
        private UUID publicId;
        private String saleNumber;
        private String customerName;
        private BigDecimal totalAmount;
        private String paymentStatus;
        private LocalDate saleDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientRecentPurchaseDTO {
        private UUID publicId;
        private String purchaseNumber;
        private String supplierName;
        private BigDecimal totalAmount;
        private String paymentStatus;
        private LocalDate purchaseDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientRecentPaymentDTO {
        private UUID publicId;
        private String paymentType; // CUSTOMER_RECEIPT or SUPPLIER_PAYMENT
        private String referenceNumber;
        private String partyName;
        private BigDecimal amount;
        private String paymentMode;
        private LocalDate paymentDate;
    }
}
