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
public class ResponseProfitDistributionDTO {

    private UUID publicId;

    private LocalDate fromDate;

    private LocalDate toDate;

    private BigDecimal totalRevenue;

    private BigDecimal totalPurchaseCost;

    private BigDecimal totalExpenses;

    private BigDecimal netProfit;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private boolean isRecalculation;

    private List<PartnerShareDetails> shares;

    public boolean getIsRecalculation() {
        return isRecalculation;
    }

    public void setIsRecalculation(boolean isRecalculation) {
        this.isRecalculation = isRecalculation;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartnerShareDetails {

        private UUID partnerPublicId;

        private String partnerName;

        private BigDecimal sharePercentageAtDistribution;

        private BigDecimal shareAmount;
    }
}
