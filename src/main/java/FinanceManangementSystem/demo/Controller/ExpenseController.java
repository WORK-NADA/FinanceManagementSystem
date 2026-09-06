package FinanceManangementSystem.demo.Controller;

import FinanceManangementSystem.demo.APIResponse.APIResponse;
import FinanceManangementSystem.demo.Enums.ExpenseCategory;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestExpenseDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseExpenseDTO;
import FinanceManangementSystem.demo.Service.ExpenseServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("expense")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseServiceInterface expenseService;


    // =========================================================
    // ADD EXPENSE
    // =========================================================

    @PostMapping("/add")
    public ResponseEntity<APIResponse<ResponseExpenseDTO>>
    addExpense(
            @Valid @RequestBody RequestExpenseDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in addExpense..."
        );

        log.info(
                "CONTROLLER - calling expense service..."
        );

        ResponseExpenseDTO response =
                expenseService.addExpense(
                        dto
                );

        log.info(
                "CONTROLLER - expense created successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new APIResponse<>(
                                "Expense recorded successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET EXPENSE BY PUBLIC ID
    // =========================================================

    @GetMapping("/{publicId}")
    public ResponseEntity<APIResponse<ResponseExpenseDTO>>
    getExpenseByPublicId(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in getExpenseByPublicId..."
        );

        log.info(
                "CONTROLLER - calling expense service..."
        );

        ResponseExpenseDTO response =
                expenseService.getExpenseByPublicId(
                        publicId
                );

        log.info(
                "CONTROLLER - expense fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Expense fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET ALL EXPENSES
    // =========================================================

    @GetMapping("/all")
    public ResponseEntity<APIResponse<org.springframework.data.domain.Page<ResponseExpenseDTO>>>
    getAllExpenses(
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("CONTROLLER - request came in getAllExpenses with filters...");
        var pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "expenseDate"));

        org.springframework.data.domain.Page<ResponseExpenseDTO> response =
                expenseService.getAllExpenses(category, fromDate, toDate, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "All active expenses fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET EXPENSES BY DATE RANGE
    // =========================================================

    @GetMapping("/date-range")
    public ResponseEntity<APIResponse<List<ResponseExpenseDTO>>>
    getExpensesByDateRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {

        log.info(
                "CONTROLLER - request came in getExpensesByDateRange..."
        );

        log.info(
                "CONTROLLER - calling expense service..."
        );

        List<ResponseExpenseDTO> response =
                expenseService.getExpensesByDateRange(
                        fromDate,
                        toDate
                );

        log.info(
                "CONTROLLER - expenses by date range fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Expenses by date range fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET EXPENSES BY CATEGORY
    // =========================================================

    @GetMapping("/category/{category}")
    public ResponseEntity<APIResponse<List<ResponseExpenseDTO>>>
    getExpensesByCategory(
            @PathVariable ExpenseCategory category
    ) {

        log.info(
                "CONTROLLER - request came in getExpensesByCategory..."
        );

        log.info(
                "CONTROLLER - calling expense service..."
        );

        List<ResponseExpenseDTO> response =
                expenseService.getExpensesByCategory(
                        category
                );

        log.info(
                "CONTROLLER - expenses by category fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Expenses by category fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // UPDATE EXPENSE
    // =========================================================

    @PutMapping("/{publicId}")
    public ResponseEntity<APIResponse<ResponseExpenseDTO>>
    updateExpense(
            @PathVariable UUID publicId,
            @Valid @RequestBody RequestExpenseDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in updateExpense..."
        );

        log.info(
                "CONTROLLER - calling expense service..."
        );

        ResponseExpenseDTO response =
                expenseService.updateExpense(
                        publicId,
                        dto
                );

        log.info(
                "CONTROLLER - expense updated successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Expense updated successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // DELETE (SOFT DELETE / DEACTIVATE) EXPENSE
    // =========================================================

    @DeleteMapping("/{publicId}")
    public ResponseEntity<APIResponse<Void>>
    deleteExpense(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in deleteExpense..."
        );

        log.info(
                "CONTROLLER - calling expense service..."
        );

        expenseService.deleteExpense(
                publicId
        );

        log.info(
                "CONTROLLER - expense deactivated successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Expense deactivated successfully...",
                                null
                        )
                );
    }


    // =========================================================
    // GET TOTAL EXPENSES (DASHBOARD SUMMARY)
    // =========================================================

    @GetMapping("/dashboard/total")
    public ResponseEntity<APIResponse<BigDecimal>>
    getTotalExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {

        log.info(
                "CONTROLLER - request came in getTotalExpenses..."
        );

        log.info(
                "CONTROLLER - calling expense service..."
        );

        BigDecimal response =
                expenseService.getTotalExpenses(
                        fromDate,
                        toDate
                );

        log.info(
                "CONTROLLER - total expenses fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Total expenses fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET CATEGORY-WISE BREAKDOWN (DASHBOARD CHART)
    // =========================================================

    @GetMapping("/dashboard/category-breakdown")
    public ResponseEntity<APIResponse<Map<ExpenseCategory, BigDecimal>>>
    getCategoryWiseBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {

        log.info(
                "CONTROLLER - request came in getCategoryWiseBreakdown..."
        );

        log.info(
                "CONTROLLER - calling expense service..."
        );

        Map<ExpenseCategory, BigDecimal> response =
                expenseService.getCategoryWiseBreakdown(
                        fromDate,
                        toDate
                );

        log.info(
                "CONTROLLER - category-wise breakdown fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Category-wise expense breakdown fetched successfully...",
                                response
                        )
                );
    }
}
