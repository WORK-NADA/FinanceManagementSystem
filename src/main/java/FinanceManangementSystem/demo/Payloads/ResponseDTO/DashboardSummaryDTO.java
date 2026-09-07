package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardSummaryDTO {

    private BigDecimal totalOutstanding;
    private BigDecimal totalReceivable;
    private BigDecimal totalExpensesThisMonth;
    private int lowStockCount;
    private ResponseProfitDistributionDTO latestProfitDistribution;
    private List<MonthlyTrendDTO> monthlyTrends = new ArrayList<>();
    private List<RecentActivityDTO> recentActivities = new ArrayList<>();
    private BigDecimal netWorkingCapital;
    private String cashFlowStatus;
    private BigDecimal openingBalance;
    private BigDecimal totalMoneyReceived;
    private BigDecimal totalMoneyPaid;
    private BigDecimal totalExpenses;
    private BigDecimal totalBalance;
    private BigDecimal totalWithdrawals;
    private BigDecimal netProfit;

    public DashboardSummaryDTO() {
    }

    public BigDecimal getTotalOutstanding() {
        return totalOutstanding;
    }

    public void setTotalOutstanding(BigDecimal totalOutstanding) {
        this.totalOutstanding = totalOutstanding;
    }

    public BigDecimal getTotalReceivable() {
        return totalReceivable;
    }

    public void setTotalReceivable(BigDecimal totalReceivable) {
        this.totalReceivable = totalReceivable;
    }

    public BigDecimal getTotalExpensesThisMonth() {
        return totalExpensesThisMonth;
    }

    public void setTotalExpensesThisMonth(BigDecimal totalExpensesThisMonth) {
        this.totalExpensesThisMonth = totalExpensesThisMonth;
    }

    public int getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(int lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public ResponseProfitDistributionDTO getLatestProfitDistribution() {
        return latestProfitDistribution;
    }

    public void setLatestProfitDistribution(ResponseProfitDistributionDTO latestProfitDistribution) {
        this.latestProfitDistribution = latestProfitDistribution;
    }

    public List<MonthlyTrendDTO> getMonthlyTrends() {
        return monthlyTrends;
    }

    public void setMonthlyTrends(List<MonthlyTrendDTO> monthlyTrends) {
        this.monthlyTrends = monthlyTrends;
    }

    public List<RecentActivityDTO> getRecentActivities() {
        return recentActivities;
    }

    public void setRecentActivities(List<RecentActivityDTO> recentActivities) {
        this.recentActivities = recentActivities;
    }

    public BigDecimal getNetWorkingCapital() {
        return netWorkingCapital;
    }

    public void setNetWorkingCapital(BigDecimal netWorkingCapital) {
        this.netWorkingCapital = netWorkingCapital;
    }

    public String getCashFlowStatus() {
        return cashFlowStatus;
    }

    public void setCashFlowStatus(String cashFlowStatus) {
        this.cashFlowStatus = cashFlowStatus;
    }

    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }

    public BigDecimal getTotalMoneyReceived() { return totalMoneyReceived; }
    public void setTotalMoneyReceived(BigDecimal totalMoneyReceived) { this.totalMoneyReceived = totalMoneyReceived; }

    public BigDecimal getTotalMoneyPaid() { return totalMoneyPaid; }
    public void setTotalMoneyPaid(BigDecimal totalMoneyPaid) { this.totalMoneyPaid = totalMoneyPaid; }

    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }

    public BigDecimal getTotalBalance() { return totalBalance; }
    public void setTotalBalance(BigDecimal totalBalance) { this.totalBalance = totalBalance; }

    public BigDecimal getTotalWithdrawals() { return totalWithdrawals; }
    public void setTotalWithdrawals(BigDecimal totalWithdrawals) { this.totalWithdrawals = totalWithdrawals; }

    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
}
