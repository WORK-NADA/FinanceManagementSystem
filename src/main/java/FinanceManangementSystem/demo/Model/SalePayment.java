package FinanceManangementSystem.demo.Model;

import FinanceManangementSystem.demo.Enums.PaymentMode;
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
        name = "sale_payments",
        indexes = {
                @Index(
                        name = "idx_sale_payment_sale",
                        columnList = "sale_id"
                ),
                @Index(
                        name = "idx_sale_payment_date",
                        columnList = "payment_date"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sale_payment_user_number",
                        columnNames = {"user_id", "payment_number"}
                )
        }
)
public class SalePayment {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "sale_id",
            nullable = false
    )
    private Sale sale;

    @Column(
            name = "amount_received",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amountReceived;

    @Column(
            name = "payment_date",
            nullable = false
    )
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_mode",
            nullable = false,
            length = 20
    )
    private PaymentMode paymentMode;

    // =========================================================
    // PAYMENT NUMBER (auto-generated, sequential, system-assigned)
    // =========================================================

    @Column(
            name = "payment_number",
            nullable = false,
            length = 30,
            updatable = false
    )
    private String paymentNumber;


    // =========================================================
    // REFERENCE NUMBER (optional, user-supplied: cheque/bank txn ID)
    // =========================================================

    @Column(
            name = "reference_number",
            nullable = true,
            length = 100
    )
    private String referenceNumber;

    @Column(
            name = "remarks",
            length = 500
    )
    private String remarks;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;
}
