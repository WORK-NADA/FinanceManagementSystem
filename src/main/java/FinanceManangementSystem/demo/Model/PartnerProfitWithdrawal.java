package FinanceManangementSystem.demo.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
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
        name = "partner_profit_withdrawals",
        indexes = {
                @Index(
                        name = "idx_withdrawal_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_withdrawal_partner",
                        columnList = "partner_id"
                ),
                @Index(
                        name = "idx_withdrawal_date",
                        columnList = "withdrawal_date"
                )
        }
)
public class PartnerProfitWithdrawal {

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

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false,
            updatable = false
    )
    private User user;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "partner_id",
            nullable = false
    )
    private Partner partner;

    @Column(
            name = "withdrawal_date",
            nullable = false
    )
    private LocalDate withdrawalDate;

    @Column(
            name = "amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "available_before_withdrawal",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal availableBeforeWithdrawal;

    @Column(
            name = "remaining_after_withdrawal",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal remainingAfterWithdrawal;

    @Column(
            name = "payment_method",
            length = 50
    )
    private String paymentMethod;

    @Column(
            name = "reference_number",
            length = 100
    )
    private String referenceNumber;

    @Column(
            name = "notes",
            length = 500
    )
    private String notes;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;
}
