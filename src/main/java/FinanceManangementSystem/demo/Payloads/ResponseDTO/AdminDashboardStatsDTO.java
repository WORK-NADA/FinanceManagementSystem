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
public class AdminDashboardStatsDTO {

    // Tenant / Client counts
    private long totalClients;
    private long activeClients;
    private long inactiveClients;

    // Platform-wide entity counts
    private long totalCustomers;
    private long totalSuppliers;
    private long totalStockItems;

    // Aggregate financial turnover & movement
    private BigDecimal totalGrossSales = BigDecimal.ZERO;
    private BigDecimal totalGrossPurchases = BigDecimal.ZERO;
    private BigDecimal totalPaymentsReceived = BigDecimal.ZERO;
    private BigDecimal totalPaymentsMade = BigDecimal.ZERO;
    private BigDecimal totalExpenses = BigDecimal.ZERO;

    // Platform working capital & balance
    private BigDecimal totalReceivables = BigDecimal.ZERO;
    private BigDecimal totalPayables = BigDecimal.ZERO;
    private BigDecimal netWorkingCapital = BigDecimal.ZERO;
    private String cashFlowStatus; // SURPLUS, DEFICIT, BALANCED

    // Top clients leaderboard
    private List<TopClientDTO> topClients;

    // Live platform activity stream
    private List<PlatformActivityDTO> recentActivities;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopClientDTO {
        private UUID publicId;
        private String ownerName;
        private String username;
        private String email;
        private String mobileNumber;
        private long totalSalesCount;
        private BigDecimal totalSalesVolume = BigDecimal.ZERO;
        private long customerCount;
        private boolean enabled;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformActivityDTO {
        private String activityType; // SALE_INVOICE, PURCHASE_BILL, PAYMENT_RECEIVED, PAYMENT_MADE, CLIENT_REGISTERED
        private String clientName;
        private String documentNumber;
        private String partyName;
        private BigDecimal amount;
        private LocalDate date;
        private String status;
    }
}
