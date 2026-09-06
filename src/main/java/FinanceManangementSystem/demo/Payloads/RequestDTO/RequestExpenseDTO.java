package FinanceManangementSystem.demo.Payloads.RequestDTO;

import FinanceManangementSystem.demo.Enums.ExpenseCategory;
import FinanceManangementSystem.demo.Enums.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestExpenseDTO {

    @NotNull(message = "Expense category is required")
    private ExpenseCategory category;

    @NotNull(message = "Amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Amount must be greater than zero"
    )
    private BigDecimal amount;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @Size(
            max = 255,
            message = "Description cannot exceed 255 characters"
    )
    private String description;

    @Size(
            max = 100,
            message = "Reference number cannot exceed 100 characters"
    )
    private String referenceNumber;

    @Size(
            max = 500,
            message = "Remarks cannot exceed 500 characters"
    )
    private String remarks;
}
