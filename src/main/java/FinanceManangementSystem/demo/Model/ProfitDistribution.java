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
        name = "profit_distributions",
        uniqueConstraints = {

                // --------------------------------------------------
                // One distribution per user per date range
                // --------------------------------------------------

                @UniqueConstraint(
                        name = "uk_distribution_user_period",
                        columnNames = {"user_id", "from_date", "to_date"}
                )
        }
)
public class ProfitDistribution {

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


    @Column(
            name = "from_date",
            nullable = false
    )
    private LocalDate fromDate;

    @Column(
            name = "to_date",
            nullable = false
    )
    private LocalDate toDate;

    @Column(
            name = "total_revenue",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal totalRevenue;

    @Column(
            name = "total_purchase_cost",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal totalPurchaseCost;

    @Column(
            name = "total_expenses",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal totalExpenses;

    @Column(
            name = "net_profit",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal netProfit;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
