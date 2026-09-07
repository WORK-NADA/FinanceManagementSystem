package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LiveProfitSharingOverviewDTO {

    private BigDecimal totalMoneyReceived;
    private BigDecimal totalMoneyPaid;
    private BigDecimal totalSalesRevenue; // Aliased to totalMoneyReceived for backward compatibility
    private BigDecimal totalPurchasesCost; // Aliased to totalMoneyPaid for backward compatibility
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;
    private BigDecimal totalDistributedProfit;
    private BigDecimal totalProfitWithdrawn;
    private BigDecimal totalRemainingProfit;
    private List<PartnerLiveProfitDTO> partners;
    private ResponseProfitDistributionDTO latestDistribution;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartnerLiveProfitDTO {
        private UUID partnerPublicId;
        private String partnerName;
        private String partnerEmail;
        private String partnerPhone;
        private BigDecimal sharePercentage;
        private boolean active;
        private BigDecimal totalEarnedProfit;
        private BigDecimal totalWithdrawnProfit;
        private BigDecimal remainingProfitAvailable;
    }
}
