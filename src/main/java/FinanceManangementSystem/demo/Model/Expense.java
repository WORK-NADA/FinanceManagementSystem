package FinanceManangementSystem.demo.Model;

import FinanceManangementSystem.demo.Enums.ExpenseCategory;
import FinanceManangementSystem.demo.Enums.PaymentMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "expenses",
        indexes = {
                @Index(
                        name = "idx_expense_category",
                        columnList = "category"
                ),
                @Index(
                        name = "idx_expense_date",
                        columnList = "expense_date"
                ),
                @Index(
                        name = "idx_expense_active",
                        columnList = "is_active"
                ),
                @Index(
                        name = "idx_expense_user",
                        columnList = "user_id"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_expense_user_number",
                        columnNames = {"user_id", "expense_number"}
                )
        }
)
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(
            name = "public_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "category",
            nullable = false,
            length = 50
    )
    private ExpenseCategory category;

    @Column(
            name = "amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "expense_date",
            nullable = false
    )
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_mode",
            nullable = false,
            length = 20
    )
    private PaymentMode paymentMode;

    @Column(
            name = "expense_number",
            nullable = false,
            length = 30,
            updatable = false
    )
    private String expenseNumber;

    @Column(
            name = "reference_number",
            length = 100
    )
    private String referenceNumber;

    @Column(
            name = "description",
            length = 255
    )
    private String description;

    @Column(
            name = "remarks",
            length = 500
    )
    private String remarks;

    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean isActive = true;

    // =========================================================
    // OWNER USER
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            updatable = false
    )
    private User user;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;
}
