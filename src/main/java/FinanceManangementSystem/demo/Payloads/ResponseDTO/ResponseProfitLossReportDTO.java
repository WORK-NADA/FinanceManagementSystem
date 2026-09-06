package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseProfitLossReportDTO {

    private BigDecimal totalRevenue;
    private BigDecimal totalPurchaseCost;
    private BigDecimal grossProfit;
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;
    private BigDecimal profitMarginPercentage;

    private Long salesCount;
    private Long purchasesCount;
    private Long expensesCount;

    private List<ExpenseCategoryBreakdownDTO> categoryBreakdown;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExpenseCategoryBreakdownDTO {
        private String category;
        private BigDecimal amount;
        private BigDecimal percentage;
    }
}
