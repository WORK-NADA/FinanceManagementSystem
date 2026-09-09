package FinanceManangementSystem.demo.Model;

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
        name = "investments",
        indexes = {
                @Index(
                        name = "idx_investment_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_investment_partner",
                        columnList = "partner_id"
                ),
                @Index(
                        name = "idx_investment_date",
                        columnList = "investment_date"
                )
        }
)
public class Investment {

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
            name = "amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "investment_date",
            nullable = false
    )
    private LocalDate investmentDate;

    @Column(
            name = "description",
            length = 500
    )
    private String description;

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
