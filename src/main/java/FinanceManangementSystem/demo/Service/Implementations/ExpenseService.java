package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Exceptions.InvalidRequestException;
import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;

import FinanceManangementSystem.demo.Enums.DocumentType;
import FinanceManangementSystem.demo.Enums.ExpenseCategory;
import FinanceManangementSystem.demo.Model.Expense;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestExpenseDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseExpenseDTO;
import FinanceManangementSystem.demo.Repository.ExpenseRepository;
import FinanceManangementSystem.demo.Service.ExpenseServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import FinanceManangementSystem.demo.Specification.ExpenseSpecification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService
        implements ExpenseServiceInterface {

    private final ExpenseRepository expenseRepo;

    private final CurrentUserService currentUserService;

    private final DocumentSequenceService documentSequenceService;

    private final ModelMapper modelMapper;


    // =========================================================
    // ADD EXPENSE
    // =========================================================

    @Override
    @Transactional
    public ResponseExpenseDTO addExpense(
            RequestExpenseDTO dto
    ) {

        log.info(
                "SERVICE - request came in addExpense..."
        );

        User currentUser = currentUserService.getCurrentUser();

        int year =
                dto.getExpenseDate()
                        .getYear();

        String expenseNumber =
                documentSequenceService
                        .generateDocumentNumber(
                                DocumentType.EXPENSE,
                                year
                        );

        Expense expense = new Expense();

        expense.setUser(currentUser);

        expense.setCategory(
                dto.getCategory()
        );

        expense.setAmount(
                dto.getAmount()
        );

        expense.setExpenseDate(
                dto.getExpenseDate()
        );

        expense.setPaymentMode(
                dto.getPaymentMode()
        );

        expense.setExpenseNumber(
                expenseNumber
        );

        if (dto.getCategory() == ExpenseCategory.OTHER) {
            if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
                throw new InvalidRequestException(
                        "Description is required when category is Other"
                );
            }
        }

        expense.setDescription(
                dto.getDescription() != null && !dto.getDescription().trim().isEmpty()
                        ? dto.getDescription().trim()
                        : null
        );

        expense.setReferenceNumber(
                dto.getReferenceNumber() != null && !dto.getReferenceNumber().isBlank()
                        ? dto.getReferenceNumber().trim()
                        : null
        );

        expense.setRemarks(
                dto.getRemarks() != null ? dto.getRemarks().trim() : null
        );

        expense.setIsActive(
                true
        );

        expense =
                expenseRepo.save(
                        expense
                );

        log.info(
                "SERVICE - expense added successfully..."
        );

        return mapToResponse(
                expense
        );
    }


    // =========================================================
    // GET EXPENSE BY PUBLIC ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ResponseExpenseDTO getExpenseByPublicId(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in getExpenseByPublicId..."
        );

        User currentUser = currentUserService.getCurrentUser();

        Expense expense =
                expenseRepo
                        .findByUserAndPublicId(
                                currentUser,
                                publicId
                        )
                        .orElseThrow(() -> {

                            log.info(
                                    "SERVICE - expense not found..."
                            );

                            return new ResourceNotFoundException(
                                    "Expense not found"
                            );
                        });

        return mapToResponse(
                expense
        );
    }


    // =========================================================
    // GET ALL EXPENSES
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ResponseExpenseDTO> getAllExpenses(org.springframework.data.domain.Pageable pageable) {
        return getAllExpenses(null, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ResponseExpenseDTO> getAllExpenses(
            ExpenseCategory category,
            LocalDate fromDate,
            LocalDate toDate,
            org.springframework.data.domain.Pageable pageable
    ) {
        log.info("SERVICE - request came in getAllExpenses with filters...");
        User currentUser = currentUserService.getCurrentUser();
        User filterUser = (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) ? null : currentUser;

        Specification<Expense> spec = ExpenseSpecification.filter(
                filterUser,
                category,
                fromDate,
                toDate
        );

        return expenseRepo.findAll(spec, pageable).map(this::mapToResponse);
    }


    // =========================================================
    // UPDATE EXPENSE
    // =========================================================

    @Override
    @Transactional
    public ResponseExpenseDTO updateExpense(
            UUID publicId,
            RequestExpenseDTO dto
    ) {

        log.info(
                "SERVICE - request came in updateExpense..."
        );

        User currentUser = currentUserService.getCurrentUser();

        Expense expense =
                expenseRepo
                        .findByUserAndPublicIdAndIsActiveTrue(
                                currentUser,
                                publicId
                        )
                        .orElseThrow(() -> {

                            log.info(
                                    "SERVICE - inactive or non-existent expense cannot be updated..."
                            );

                            return new ResourceNotFoundException(
                                    "Inactive expense cannot be updated"
                            );
                        });

        expense.setCategory(
                dto.getCategory()
        );

        expense.setAmount(
                dto.getAmount()
        );

        expense.setExpenseDate(
                dto.getExpenseDate()
        );

        expense.setPaymentMode(
                dto.getPaymentMode()
        );

        if (dto.getCategory() == ExpenseCategory.OTHER) {
            if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
                throw new InvalidRequestException(
                        "Description is required when category is Other"
                );
            }
        }

        expense.setDescription(
                dto.getDescription() != null && !dto.getDescription().trim().isEmpty()
                        ? dto.getDescription().trim()
                        : null
        );

        expense.setReferenceNumber(
                dto.getReferenceNumber() != null && !dto.getReferenceNumber().isBlank()
                        ? dto.getReferenceNumber().trim()
                        : null
        );

        expense.setRemarks(
                dto.getRemarks() != null ? dto.getRemarks().trim() : null
        );

        expense =
                expenseRepo.save(
                        expense
                );

        log.info(
                "SERVICE - expense updated successfully..."
        );

        return mapToResponse(
                expense
        );
    }


    // =========================================================
    // DELETE (SOFT DELETE / DEACTIVATE) EXPENSE
    // =========================================================

    @Override
    @Transactional
    public void deleteExpense(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in deleteExpense..."
        );

        User currentUser = currentUserService.getCurrentUser();

        Expense expense =
                expenseRepo
                        .findByUserAndPublicId(
                                currentUser,
                                publicId
                        )
                        .orElseThrow(() -> new InvalidRequestException(
                                "Expense not found"
                        ));

        if (!expense.getIsActive()) {

            log.info(
                    "SERVICE - expense is already inactive..."
            );

            throw new InvalidRequestException(
                    "Expense is already inactive"
            );
        }

        expense.setIsActive(
                false
        );

        expenseRepo.save(
                expense
        );

        log.info(
                "SERVICE - expense deactivated successfully..."
        );
    }


    // =========================================================
    // GET EXPENSES BY DATE RANGE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseExpenseDTO> getExpensesByDateRange(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        log.info(
                "SERVICE - request came in getExpensesByDateRange..."
        );

        if (fromDate == null) fromDate = LocalDate.now().withDayOfMonth(1);
        if (toDate == null) toDate = LocalDate.now();

        User currentUser = currentUserService.getCurrentUser();

        return expenseRepo
                .findByUserAndExpenseDateBetweenAndIsActiveTrueOrderByExpenseDateDesc(
                        currentUser,
                        fromDate,
                        toDate
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET EXPENSES BY CATEGORY
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseExpenseDTO> getExpensesByCategory(
            ExpenseCategory category
    ) {

        log.info(
                "SERVICE - request came in getExpensesByCategory..."
        );

        User currentUser = currentUserService.getCurrentUser();

        return expenseRepo
                .findByUserAndCategoryAndIsActiveTrueOrderByExpenseDateDesc(
                        currentUser,
                        category
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET TOTAL EXPENSES (DASHBOARD SUMMARY)
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenses(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        log.info(
                "SERVICE - request came in getTotalExpenses..."
        );

        User currentUser = currentUserService.getCurrentUser();
        BigDecimal result;
        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            result = expenseRepo.sumTotalExpensesByDateRange(
                    fromDate,
                    toDate
            );
        } else {
            result = expenseRepo.sumTotalExpensesByUserAndDateRange(
                    currentUser,
                    fromDate,
                    toDate
            );
        }
        return result == null ? BigDecimal.ZERO : result;
    }


    // =========================================================
    // GET TOTAL EXPENSES BY USER (PROFIT DISTRIBUTION SCOPING)
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenses(
            User user,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        log.info(
                "SERVICE - request came in getTotalExpenses (user-scoped)..."
        );

        BigDecimal result = expenseRepo.sumTotalExpensesByUserAndDateRange(
                user,
                fromDate,
                toDate
        );
        return result == null ? java.math.BigDecimal.ZERO : result;
    }


    // =========================================================
    // GET CATEGORY-WISE BREAKDOWN (DASHBOARD CHART)
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Map<ExpenseCategory, BigDecimal> getCategoryWiseBreakdown(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        log.info(
                "SERVICE - request came in getCategoryWiseBreakdown..."
        );

        Map<ExpenseCategory, BigDecimal> breakdown =
                new EnumMap<>(ExpenseCategory.class);

        for (ExpenseCategory cat : ExpenseCategory.values()) {
            breakdown.put(cat, BigDecimal.ZERO);
        }

        User currentUser = currentUserService.getCurrentUser();
        List<Object[]> results;
        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            results = expenseRepo.findCategoryWiseBreakdownByDateRange(
                    fromDate,
                    toDate
            );
        } else {
            results = expenseRepo.findCategoryWiseBreakdownByUserAndDateRange(
                    currentUser,
                    fromDate,
                    toDate
            );
        }

        for (Object[] row : results) {

            ExpenseCategory category =
                    (ExpenseCategory) row[0];

            BigDecimal totalAmount =
                    (BigDecimal) row[1];

            if (category != null && totalAmount != null) {
                breakdown.put(category, totalAmount);
            }
        }

        return breakdown;
    }


    // =========================================================
    // HELPER: MAP TO RESPONSE DTO
    // =========================================================

    private ResponseExpenseDTO mapToResponse(
            Expense expense
    ) {

        return modelMapper.map(
                expense,
                ResponseExpenseDTO.class
        );
    }
}
