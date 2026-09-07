package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import java.math.BigDecimal;

public class MonthlyTrendDTO {

    private String month;
    private BigDecimal revenue;
    private BigDecimal cost;
    private BigDecimal expenses;
    private BigDecimal profit;

    public MonthlyTrendDTO() {}

    public MonthlyTrendDTO(String month, BigDecimal revenue, BigDecimal cost, BigDecimal expenses, BigDecimal profit) {
        this.month = month;
        this.revenue = revenue;
        this.cost = cost;
        this.expenses = expenses;
        this.profit = profit;
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public BigDecimal getExpenses() { return expenses; }
    public void setExpenses(BigDecimal expenses) { this.expenses = expenses; }

    public BigDecimal getProfit() { return profit; }
    public void setProfit(BigDecimal profit) { this.profit = profit; }
}
