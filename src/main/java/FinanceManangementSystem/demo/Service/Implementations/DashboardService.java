package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Model.Sale;
import FinanceManangementSystem.demo.Model.Purchase;
import FinanceManangementSystem.demo.Model.Expense;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.DashboardSummaryDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.MonthlyTrendDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.RecentActivityDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseProfitDistributionDTO;
import FinanceManangementSystem.demo.Exceptions.InvalidRequestException;
import FinanceManangementSystem.demo.Repository.ExpenseRepository;
import FinanceManangementSystem.demo.Repository.PurchasePaymentRepository;
import FinanceManangementSystem.demo.Repository.PurchaseRepository;
import FinanceManangementSystem.demo.Repository.SalePaymentRepository;
import FinanceManangementSystem.demo.Repository.SaleRepository;
import FinanceManangementSystem.demo.Repository.UserRepository;
import FinanceManangementSystem.demo.Service.DashboardServiceInterface;
import FinanceManangementSystem.demo.Service.ExpenseServiceInterface;
import FinanceManangementSystem.demo.Service.PurchasePaymentServiceInterface;
import FinanceManangementSystem.demo.Service.SalePaymentServiceInterface;
import FinanceManangementSystem.demo.Service.StockServiceInterface;
import FinanceManangementSystem.demo.Service.ProfitDistributionServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService implements DashboardServiceInterface {

    private final PurchasePaymentServiceInterface purchasePaymentService;
    private final SalePaymentServiceInterface salePaymentService;
    private final ExpenseServiceInterface expenseService;
    private final StockServiceInterface stockService;
    private final ProfitDistributionServiceInterface profitDistributionService;
    private final SaleRepository saleRepo;
    private final PurchaseRepository purchaseRepo;
    private final ExpenseRepository expenseRepo;
    private final SalePaymentRepository salePaymentRepo;
    private final PurchasePaymentRepository purchasePaymentRepo;
    private final UserRepository userRepo;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary() {
        log.info("SERVICE - request came in getDashboardSummary...");

        User currentUser = currentUserService.getCurrentUser();
        DashboardSummaryDTO dto = new DashboardSummaryDTO();

        // 1. Core Receivables and Payables
        BigDecimal outstanding = purchasePaymentService.getTotalOutstandingAmount();
        BigDecimal receivable = salePaymentService.getTotalReceivableAmount();
        dto.setTotalOutstanding(outstanding != null ? outstanding : BigDecimal.ZERO);
        dto.setTotalReceivable(receivable != null ? receivable : BigDecimal.ZERO);

        // 2. Net Working Capital & Cash Flow Status
        BigDecimal netWorkingCapital = dto.getTotalReceivable().subtract(dto.getTotalOutstanding());
        dto.setNetWorkingCapital(netWorkingCapital);
        if (netWorkingCapital.compareTo(BigDecimal.ZERO) > 0) {
            dto.setCashFlowStatus("SURPLUS");
        } else if (netWorkingCapital.compareTo(BigDecimal.ZERO) < 0) {
            dto.setCashFlowStatus("DEFICIT");
        } else {
            dto.setCashFlowStatus("BALANCED");
        }

        // 3. Continuous Total Balance Calculation based on actual money movement
        // Total Balance = Opening Balance + Total Money Received - Total Money Paid - Total Expenses
        BigDecimal openingBalance = currentUser.getOpeningBalance() != null ? currentUser.getOpeningBalance() : BigDecimal.ZERO;
        BigDecimal totalReceived = salePaymentRepo.sumTotalReceivedByUser(currentUser);
        if (totalReceived == null) totalReceived = BigDecimal.ZERO;

        BigDecimal totalPaid = purchasePaymentRepo.sumTotalPaidByUser(currentUser);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        BigDecimal totalExpenses = expenseRepo.sumTotalExpensesByUserAndDateRange(currentUser, null, null);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        BigDecimal totalBalance = openingBalance.add(totalReceived).subtract(totalPaid).subtract(totalExpenses);

        dto.setOpeningBalance(openingBalance);
        dto.setTotalMoneyReceived(totalReceived);
        dto.setTotalMoneyPaid(totalPaid);
        dto.setTotalExpenses(totalExpenses);
        dto.setTotalBalance(totalBalance);

        // 4. Current Month OPEX
        YearMonth currentYm = YearMonth.now();
        LocalDate from = currentYm.atDay(1);
        LocalDate to = LocalDate.now();
        dto.setTotalExpensesThisMonth(expenseService.getTotalExpenses(from, to));

        // 4. Low Stock Count
        dto.setLowStockCount(stockService.getLowStockList().size());

        // 5. Latest Profit Distribution
        ResponseProfitDistributionDTO latest = profitDistributionService.getLatestDistribution();
        dto.setLatestProfitDistribution(latest);

        // 6. Consolidated 6-Month Trend Data (Eliminating Frontend Network Waterfall)
        List<MonthlyTrendDTO> trends = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = currentYm.minusMonths(i);
            LocalDate mStart = ym.atDay(1);
            LocalDate mEnd = ym.atEndOfMonth();

            BigDecimal mRev = saleRepo.findByUserAndSaleDateBetween(currentUser, mStart, mEnd)
                    .stream()
                    .map(s -> s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal mCost = purchaseRepo.findByUserAndPurchaseDateBetween(currentUser, mStart, mEnd)
                    .stream()
                    .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal mExp = expenseRepo.sumTotalExpensesByUserAndDateRange(currentUser, mStart, mEnd);
            if (mExp == null)
                mExp = BigDecimal.ZERO;

            BigDecimal mProfit = mRev.subtract(mCost).subtract(mExp);

            trends.add(new MonthlyTrendDTO(ym.toString(), mRev, mCost, mExp, mProfit));
        }
        dto.setMonthlyTrends(trends);

        // 7. Recent Operational Activity Feed (Sales, Purchases, Expenses)
        List<RecentActivityDTO> activities = new ArrayList<>();

        List<Sale> recentSales = saleRepo
                .findByUser(currentUser, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "saleDate", "id")))
                .getContent();
        for (Sale s : recentSales) {
            String party = s.getCustomer() != null ? s.getCustomer().getCustomerName() : "Customer";
            String status = s.getPaymentStatus() != null ? s.getPaymentStatus().name() : "PENDING";
            activities.add(new RecentActivityDTO("SALE_INVOICE", s.getSaleNumber(), party, s.getTotalAmount(),
                    s.getSaleDate(), status));
        }

        List<Purchase> recentPurchases = purchaseRepo
                .findByUser(currentUser, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "purchaseDate", "id")))
                .getContent();
        for (Purchase p : recentPurchases) {
            String party = p.getSupplier() != null ? p.getSupplier().getSupplierName() : "Supplier";
            String status = p.getPaymentStatus() != null ? p.getPaymentStatus().name() : "PENDING";
            activities.add(new RecentActivityDTO("PURCHASE_BILL", p.getPurchaseNumber(), party, p.getTotalAmount(),
                    p.getPurchaseDate(), status));
        }

        List<Expense> recentExpenses = expenseRepo
                .findByUserAndIsActiveTrueOrderByExpenseDateDesc(currentUser, PageRequest.of(0, 5)).getContent();
        for (Expense e : recentExpenses) {
            String party = e.getCategory() != null ? e.getCategory().name().replace('_', ' ') : "Operating Expense";
            String doc = e.getExpenseNumber() != null ? e.getExpenseNumber() : "EXP";
            activities.add(new RecentActivityDTO("EXPENSE", doc, party, e.getAmount(), e.getExpenseDate(), "PAID"));
        }

        // Sort all activities by date descending and limit to top 8
        activities.sort(
                Comparator.comparing(RecentActivityDTO::getDate, Comparator.nullsLast(Comparator.reverseOrder())));
        if (activities.size() > 8) {
            activities = activities.subList(0, 8);
        }
        dto.setRecentActivities(activities);

        return dto;
    }

    @Override
    @Transactional
    public DashboardSummaryDTO updateOpeningBalance(BigDecimal openingBalance) {
        log.info("SERVICE - request came in updateOpeningBalance: {}", openingBalance);

        if (openingBalance == null || openingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("Opening balance must be greater than or equal to 0");
        }

        User currentUser = currentUserService.getCurrentUser();
        currentUser.setOpeningBalance(openingBalance);
        userRepo.save(currentUser);

        return getDashboardSummary();
    }
}
