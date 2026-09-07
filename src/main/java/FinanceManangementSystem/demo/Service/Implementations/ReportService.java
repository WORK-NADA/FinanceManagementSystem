package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Model.*;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.*;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseProfitLossReportDTO.ExpenseCategoryBreakdownDTO;
import FinanceManangementSystem.demo.Repository.*;
import FinanceManangementSystem.demo.Service.ReportServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService implements ReportServiceInterface {

    private final CurrentUserService currentUserService;
    private final SaleRepository saleRepo;
    private final PurchaseRepository purchaseRepo;
    private final ExpenseRepository expenseRepo;
    private final StockRepository stockRepo;
    private final CustomerRepository customerRepo;
    private final SupplierRepository supplierRepo;
    private final SalePaymentRepository salePaymentRepo;
    private final PurchasePaymentRepository purchasePaymentRepo;

    @Override
    @Transactional(readOnly = true)
    public List<ResponseSaleReportDTO> getSalesReport(LocalDate fromDate, LocalDate toDate) {
        log.info("SERVICE - request came in getSalesReport...");
        User currentUser = currentUserService.getCurrentUser();
        List<Sale> sales = saleRepo.findByUserAndSaleDateBetween(currentUser, fromDate, toDate);

        return sales.stream().map(s -> {
            ResponseSaleReportDTO dto = new ResponseSaleReportDTO();
            dto.setPublicId(s.getPublicId());
            dto.setSaleDate(s.getSaleDate());
            dto.setTotalAmount(s.getTotalAmount());
            dto.setPaymentStatus(s.getPaymentStatus());
            dto.setCustomerName(s.getCustomer() != null ? s.getCustomer().getCustomerName() : null);
            dto.setSaleNumber(s.getSaleNumber());
            dto.setCustomerInvoiceNumber(s.getCustomerInvoiceNumber());
            dto.setRawMaterial(s.getRawMaterial());
            dto.setWeight(s.getWeight());
            dto.setUnit(s.getUnit() != null ? s.getUnit().name() : "KG");
            dto.setRatePerUnit(s.getRatePerUnit());
            return dto;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponsePurchaseReportDTO> getPurchaseReport(LocalDate fromDate, LocalDate toDate) {
        log.info("SERVICE - request came in getPurchaseReport...");
        User currentUser = currentUserService.getCurrentUser();
        List<Purchase> purchases = purchaseRepo.findByUserAndPurchaseDateBetween(currentUser, fromDate, toDate);

        return purchases.stream().map(p -> {
            ResponsePurchaseReportDTO dto = new ResponsePurchaseReportDTO();
            dto.setPublicId(p.getPublicId());
            dto.setPurchaseDate(p.getPurchaseDate());
            dto.setTotalAmount(p.getTotalAmount());
            dto.setPaymentStatus(p.getPaymentStatus());
            dto.setSupplierName(p.getSupplier() != null ? p.getSupplier().getSupplierName() : null);
            dto.setPurchaseNumber(p.getPurchaseNumber());
            dto.setSupplierInvoiceNumber(p.getSupplierInvoiceNumber());
            dto.setRawMaterial(p.getRawMaterial());
            dto.setWeight(p.getWeight());
            dto.setUnit(p.getUnit() != null ? p.getUnit().name() : "KG");
            dto.setRatePerUnit(p.getRatePerUnit());
            return dto;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseExpenseReportDTO> getExpenseReport(LocalDate fromDate, LocalDate toDate) {
        log.info("SERVICE - request came in getExpenseReport...");
        User currentUser = currentUserService.getCurrentUser();
        return expenseRepo.findByUserAndExpenseDateBetweenAndIsActiveTrueOrderByExpenseDateDesc(currentUser, fromDate, toDate)
                .stream()
                .map(e -> {
                    ResponseExpenseReportDTO dto = new ResponseExpenseReportDTO();
                    dto.setPublicId(e.getPublicId());
                    dto.setExpenseDate(e.getExpenseDate());
                    dto.setAmount(e.getAmount());
                    dto.setDescription(e.getDescription());
                    dto.setExpenseNumber(e.getExpenseNumber());
                    dto.setCategory(e.getCategory());
                    dto.setPaymentMode(e.getPaymentMode());
                    return dto;
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseProfitLossReportDTO getProfitLossReport(LocalDate fromDate, LocalDate toDate) {
        log.info("SERVICE - request came in getProfitLossReport...");
        User currentUser = currentUserService.getCurrentUser();

        // Revenue = actual cash received from customers in the date range (payment-based)
        BigDecimal totalRevenue = salePaymentRepo.sumTotalReceivedByUserAndDateRange(currentUser, fromDate, toDate);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        // Purchase Cost = actual cash paid to suppliers in the date range (payment-based)
        BigDecimal totalPurchaseCost = purchasePaymentRepo.sumTotalPaidByUserAndDateRange(currentUser, fromDate, toDate);
        if (totalPurchaseCost == null) totalPurchaseCost = BigDecimal.ZERO;

        List<Expense> expenses = expenseRepo.findByUserAndExpenseDateBetweenAndIsActiveTrueOrderByExpenseDateDesc(currentUser, fromDate, toDate);
        BigDecimal totalExpenses = expenses.stream()
                .map(e -> e.getAmount() == null ? BigDecimal.ZERO : e.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossProfit = totalRevenue.subtract(totalPurchaseCost);
        BigDecimal netProfit = grossProfit.subtract(totalExpenses);

        BigDecimal profitMarginPercentage = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            profitMarginPercentage = netProfit.multiply(new BigDecimal("100"))
                    .divide(totalRevenue, 2, RoundingMode.HALF_UP);
        }

        // Aggregate expense category breakdown
        Map<String, BigDecimal> categorySums = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory().name() : "OTHER",
                        Collectors.reducing(BigDecimal.ZERO, e -> e.getAmount() == null ? BigDecimal.ZERO : e.getAmount(), BigDecimal::add)
                ));

        BigDecimal finalTotalExpenses = totalExpenses;
        List<ExpenseCategoryBreakdownDTO> breakdown = categorySums.entrySet().stream()
                .map(entry -> {
                    BigDecimal catAmount = entry.getValue();
                    BigDecimal pct = finalTotalExpenses.compareTo(BigDecimal.ZERO) > 0
                            ? catAmount.multiply(new BigDecimal("100")).divide(finalTotalExpenses, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return ExpenseCategoryBreakdownDTO.builder()
                            .category(entry.getKey())
                            .amount(catAmount)
                            .percentage(pct)
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .toList();

        // Use invoice-list counts for informational purposes (unchanged)
        List<Sale> sales = saleRepo.findByUserAndSaleDateBetween(currentUser, fromDate, toDate);
        List<Purchase> purchases = purchaseRepo.findByUserAndPurchaseDateBetween(currentUser, fromDate, toDate);

        return ResponseProfitLossReportDTO.builder()
                .totalRevenue(totalRevenue)
                .totalPurchaseCost(totalPurchaseCost)
                .grossProfit(grossProfit)
                .totalExpenses(totalExpenses)
                .netProfit(netProfit)
                .profitMarginPercentage(profitMarginPercentage)
                .salesCount((long) sales.size())
                .purchasesCount((long) purchases.size())
                .expensesCount((long) expenses.size())
                .categoryBreakdown(breakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseStockReportDTO> getStockReport() {
        log.info("SERVICE - request came in getStockReport...");
        User currentUser = currentUserService.getCurrentUser();
        List<Stock> stocks = stockRepo.findByUser(currentUser);
        List<Purchase> userPurchases = purchaseRepo.findByUser(currentUser);

        // Map latest purchase rate per raw material
        Map<String, BigDecimal> latestRateMap = new HashMap<>();
        for (Purchase p : userPurchases) {
            if (p.getRawMaterial() != null && p.getRatePerUnit() != null) {
                latestRateMap.putIfAbsent(p.getRawMaterial().trim().toLowerCase(), p.getRatePerUnit());
            }
        }

        return stocks.stream().map(s -> {
            String rmKey = s.getRawMaterial() != null ? s.getRawMaterial().trim().toLowerCase() : "";
            BigDecimal rate = latestRateMap.getOrDefault(rmKey, BigDecimal.ZERO);
            BigDecimal qty = s.getCurrentQuantity() != null ? s.getCurrentQuantity() : BigDecimal.ZERO;
            BigDecimal valuation = qty.multiply(rate);

            String status = "HEALTHY";
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                status = "OUT_OF_STOCK";
            } else if (s.getMinimumStockLevel() != null && qty.compareTo(s.getMinimumStockLevel()) <= 0) {
                status = "LOW_STOCK";
            }

            return ResponseStockReportDTO.builder()
                    .publicId(s.getPublicId())
                    .rawMaterial(s.getRawMaterial())
                    .unit(s.getUnit())
                    .currentQuantity(qty)
                    .minimumStockLevel(s.getMinimumStockLevel())
                    .valuationRate(rate)
                    .totalValuation(valuation)
                    .stockStatus(status)
                    .build();
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseCustomerOutstandingReportDTO> getCustomerOutstandingReport() {
        log.info("SERVICE - request came in getCustomerOutstandingReport...");
        User currentUser = currentUserService.getCurrentUser();
        List<Customer> customers = customerRepo.findByUser(currentUser);
        List<ResponseCustomerOutstandingReportDTO> result = new ArrayList<>();

        for (Customer c : customers) {
            BigDecimal opening = c.getOpeningBalance() != null ? c.getOpeningBalance() : BigDecimal.ZERO;
            List<Sale> sales = saleRepo.findByCustomer(c);
            BigDecimal totalInvoiced = sales.stream()
                    .map(s -> s.getTotalAmount() == null ? BigDecimal.ZERO : s.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<SalePayment> payments = salePaymentRepo.findByUserAndSale_Customer_PublicIdOrderByPaymentDateDesc(currentUser, c.getPublicId());
            BigDecimal totalReceived = payments.stream()
                    .map(p -> p.getAmountReceived() == null ? BigDecimal.ZERO : p.getAmountReceived())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netOutstanding = opening.add(totalInvoiced).subtract(totalReceived);

            if (netOutstanding.compareTo(BigDecimal.ZERO) > 0) {
                LocalDate lastTxn = null;
                if (!sales.isEmpty()) {
                    lastTxn = sales.stream().map(Sale::getSaleDate).max(LocalDate::compareTo).orElse(null);
                }
                String city = c.getAddress() != null ? c.getAddress().getCity() : null;

                result.add(ResponseCustomerOutstandingReportDTO.builder()
                        .customerPublicId(c.getPublicId())
                        .customerName(c.getCustomerName())
                        .mobileNumber(c.getMobileNumber())
                        .email(c.getEmail())
                        .city(city)
                        .totalInvoiced(totalInvoiced)
                        .totalReceived(totalReceived)
                        .outstandingAmount(netOutstanding)
                        .lastTransactionDate(lastTxn)
                        .build());
            }
        }

        result.sort((a, b) -> b.getOutstandingAmount().compareTo(a.getOutstandingAmount()));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseSupplierOutstandingReportDTO> getSupplierOutstandingReport() {
        log.info("SERVICE - request came in getSupplierOutstandingReport...");
        User currentUser = currentUserService.getCurrentUser();
        List<Supplier> suppliers = supplierRepo.findByUser(currentUser);
        List<ResponseSupplierOutstandingReportDTO> result = new ArrayList<>();

        for (Supplier s : suppliers) {
            BigDecimal opening = s.getOpeningBalance() != null ? s.getOpeningBalance() : BigDecimal.ZERO;
            List<Purchase> purchases = purchaseRepo.findBySupplier(s);
            BigDecimal totalBilled = purchases.stream()
                    .map(p -> p.getTotalAmount() == null ? BigDecimal.ZERO : p.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<PurchasePayment> payments = purchasePaymentRepo.findByUserAndPurchase_Supplier_PublicIdOrderByPaymentDateDesc(currentUser, s.getPublicId());
            BigDecimal totalPaid = payments.stream()
                    .map(p -> p.getAmountPaid() == null ? BigDecimal.ZERO : p.getAmountPaid())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netOutstanding = opening.add(totalBilled).subtract(totalPaid);

            if (netOutstanding.compareTo(BigDecimal.ZERO) > 0) {
                LocalDate lastTxn = null;
                if (!purchases.isEmpty()) {
                    lastTxn = purchases.stream().map(Purchase::getPurchaseDate).max(LocalDate::compareTo).orElse(null);
                }
                String city = s.getAddress() != null ? s.getAddress().getCity() : null;

                result.add(ResponseSupplierOutstandingReportDTO.builder()
                        .supplierPublicId(s.getPublicId())
                        .supplierName(s.getSupplierName())
                        .mobileNumber(s.getMobileNumber())
                        .email(s.getEmail())
                        .city(city)
                        .totalBilled(totalBilled)
                        .totalPaid(totalPaid)
                        .outstandingAmount(netOutstanding)
                        .lastTransactionDate(lastTxn)
                        .build());
            }
        }

        result.sort((a, b) -> b.getOutstandingAmount().compareTo(a.getOutstandingAmount()));
        return result;
    }
}

