package FinanceManangementSystem.demo.Model;

import FinanceManangementSystem.demo.Enums.WeightUnit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "stocks",

        indexes = {

                // --------------------------------------------------
                // Search / Filtering
                // --------------------------------------------------

                @Index(
                        name = "idx_stock_raw_material",
                        columnList = "raw_material"
                ),

                @Index(
                        name = "idx_stock_unit",
                        columnList = "unit"
                ),

                @Index(
                        name = "idx_stock_active",
                        columnList = "is_active"
                ),
                @Index(
                        name = "idx_stock_user",
                        columnList = "user_id"
                )
        },

        uniqueConstraints = {

                // --------------------------------------------------
                // One Stock Master Per User + Raw Material + Unit
                // --------------------------------------------------

                @UniqueConstraint(
                        name = "uk_stock_user_raw_material_unit",
                        columnNames = {
                                "user_id",
                                "raw_material",
                                "unit"
                        }
                )
        }
)
public class Stock {

    // =========================================================
    // PRIMARY KEY
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // PUBLIC ID
    // =========================================================

    @UuidGenerator
    @Column(
            name = "public_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID publicId;


    // =========================================================
    // RAW MATERIAL
    // =========================================================

    @Column(
            name = "raw_material",
            nullable = false,
            length = 150
    )
    private String rawMaterial;


    // =========================================================
    // UNIT
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "unit",
            nullable = false,
            length = 20
    )
    private WeightUnit unit = WeightUnit.KG;


    // =========================================================
    // CURRENT STOCK QUANTITY
    // =========================================================

    @Column(
            name = "current_quantity",
            nullable = false,
            precision = 15,
            scale = 3
    )
    private BigDecimal currentQuantity = BigDecimal.ZERO;


    // =========================================================
    // MINIMUM STOCK LEVEL
    // =========================================================

    @Column(
            name = "minimum_stock_level",
            nullable = false,
            precision = 15,
            scale = 3
    )
    private BigDecimal minimumStockLevel = BigDecimal.ZERO;


    // =========================================================
    // ACTIVE / INACTIVE
    // =========================================================

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


    // =========================================================
    // AUDIT
    // =========================================================

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

    // =========================================================
    // OPTIMISTIC LOCKING VERSION
    // =========================================================

    @Version
    @Column(name = "version")
    private Long version;
}