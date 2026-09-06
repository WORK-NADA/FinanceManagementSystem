package FinanceManangementSystem.demo.Service;

import FinanceManangementSystem.demo.Enums.ExpenseCategory;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestExpenseDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseExpenseDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ExpenseServiceInterface {

    ResponseExpenseDTO addExpense(
            RequestExpenseDTO dto
    );

    ResponseExpenseDTO getExpenseByPublicId(
            UUID publicId
    );

    default org.springframework.data.domain.Page<ResponseExpenseDTO> getAllExpenses(org.springframework.data.domain.Pageable pageable) {
        return getAllExpenses(null, null, null, pageable);
    }

    org.springframework.data.domain.Page<ResponseExpenseDTO> getAllExpenses(
            ExpenseCategory category,
            LocalDate fromDate,
            LocalDate toDate,
            org.springframework.data.domain.Pageable pageable
    );

    ResponseExpenseDTO updateExpense(
            UUID publicId,
            RequestExpenseDTO dto
    );

    void deleteExpense(
            UUID publicId
    );

    List<ResponseExpenseDTO> getExpensesByDateRange(
            LocalDate fromDate,
            LocalDate toDate
    );

    List<ResponseExpenseDTO> getExpensesByCategory(
            ExpenseCategory category
    );

    BigDecimal getTotalExpenses(
            LocalDate fromDate,
            LocalDate toDate
    );

    BigDecimal getTotalExpenses(
            User user,
            LocalDate fromDate,
            LocalDate toDate
    );

    Map<ExpenseCategory, BigDecimal> getCategoryWiseBreakdown(
            LocalDate fromDate,
            LocalDate toDate
    );
}