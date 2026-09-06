package FinanceManangementSystem.demo.Repository;

import FinanceManangementSystem.demo.Enums.WeightUnit;
import FinanceManangementSystem.demo.Model.Stock;
import FinanceManangementSystem.demo.Model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository
        extends JpaRepository<Stock, Long> {

    // =========================================================
    // FIND BY PUBLIC ID
    // =========================================================

    Optional<Stock> findByPublicId(
            UUID publicId
    );

    Optional<Stock> findByUserAndPublicId(User user, UUID publicId);

    Optional<Stock> findByUserAndRawMaterialIgnoreCaseAndUnit(User user, String rawMaterial, WeightUnit unit);

    List<Stock> findByUser(User user);

    List<Stock> findByUserAndIsActiveTrue(User user);

    List<Stock> findByUserAndIsActiveFalse(User user);

    // =========================================================
    // FIND ACTIVE STOCK BY PUBLIC ID
    // =========================================================

    Optional<Stock> findByPublicIdAndIsActiveTrue(
            UUID publicId
    );


    // =========================================================
    // FIND BY RAW MATERIAL AND UNIT
    // =========================================================

    Optional<Stock> findByRawMaterialIgnoreCaseAndUnit(
            String rawMaterial,
            WeightUnit unit
    );


    // =========================================================
    // CHECK EXISTING STOCK
    // =========================================================

    boolean existsByRawMaterialIgnoreCaseAndUnit(
            String rawMaterial,
            WeightUnit unit
    );

    boolean existsByUserAndRawMaterialIgnoreCaseAndUnit(
            User user,
            String rawMaterial,
            WeightUnit unit
    );

    @Query("""
            SELECT COUNT(s) > 0
            FROM Stock s
            WHERE s.user = :user
            AND LOWER(s.rawMaterial) = LOWER(:rawMaterial)
            """)
    boolean existsByUserAndRawMaterialIgnoreCase(
            @Param("user") User user,
            @Param("rawMaterial") String rawMaterial
    );

    @Query("""
            SELECT COUNT(s) > 0
            FROM Stock s
            WHERE s.user = :user
            AND LOWER(s.rawMaterial) = LOWER(:rawMaterial)
            AND s.publicId <> :publicId
            """)
    boolean existsByUserAndRawMaterialIgnoreCaseAndPublicIdNot(
            @Param("user") User user,
            @Param("rawMaterial") String rawMaterial,
            @Param("publicId") UUID publicId
    );

    @Query("""
            SELECT s
            FROM Stock s
            WHERE s.user = :user
            AND LOWER(s.rawMaterial) = LOWER(:rawMaterial)
            """)
    Optional<Stock> findByUserAndRawMaterialIgnoreCase(
            @Param("user") User user,
            @Param("rawMaterial") String rawMaterial
    );


    // =========================================================
    // ACTIVE STOCKS
    // =========================================================

    List<Stock> findByIsActiveTrue();


    // =========================================================
    // INACTIVE STOCKS
    // =========================================================

    List<Stock> findByIsActiveFalse();


    // =========================================================
    // ALL STOCKS SEARCH
    // =========================================================

    List<Stock> findByRawMaterialContainingIgnoreCase(
            String rawMaterial
    );


    // =========================================================
    // ACTIVE STOCK SEARCH
    // =========================================================

    List<Stock>
    findByUserAndRawMaterialContainingIgnoreCaseAndIsActiveTrue(
            User user,
            String rawMaterial
    );

    List<Stock>
    findByRawMaterialContainingIgnoreCaseAndIsActiveTrue(
            String rawMaterial
    );


    // =========================================================
    // INACTIVE STOCK SEARCH
    // =========================================================

    List<Stock>
    findByUserAndRawMaterialContainingIgnoreCaseAndIsActiveFalse(
            User user,
            String rawMaterial
    );

    List<Stock>
    findByRawMaterialContainingIgnoreCaseAndIsActiveFalse(
            String rawMaterial
    );


    // =========================================================
    // STOCK AUDIT / RECONCILIATION
    // =========================================================
    /*
     * Finds stocks whose recorded currentQuantity does not match
     * the sum of transactions in the ledger.
     *
     * In a healthy system, this query should return 0 results.
     */

    @Query("""
            SELECT s
            FROM Stock s
            WHERE s.currentQuantity <> (
                SELECT COALESCE(SUM(
                    CASE
                        WHEN t.transactionType IN (
                            FinanceManangementSystem.demo.Enums.StockTransactionType.PURCHASE_IN,
                            FinanceManangementSystem.demo.Enums.StockTransactionType.ADJUSTMENT_IN,
                            FinanceManangementSystem.demo.Enums.StockTransactionType.CANCEL_SALE_IN
                        ) THEN t.quantity
                        WHEN t.transactionType IN (
                            FinanceManangementSystem.demo.Enums.StockTransactionType.SALE_OUT,
                            FinanceManangementSystem.demo.Enums.StockTransactionType.ADJUSTMENT_OUT,
                            FinanceManangementSystem.demo.Enums.StockTransactionType.CANCEL_PURCHASE_OUT
                        ) THEN -t.quantity
                        ELSE 0
                    END
                ), 0)
                FROM StockTransaction t
                WHERE t.stock = s
            )
            """)
    List<Stock> findDiscrepantStocks();


    // =========================================================
    // FIND LOW STOCKS
    // =========================================================

    /**
     * Find active stocks where the current quantity is less than or equal to the
     * configured minimum stock level.
     *
     * NOTE: This method uses an explicit JPQL {@code @Query} because the
     * predicate compares two entity fields ({@code currentQuantity} and
     * {@code minimumStockLevel}). That comparison cannot be expressed using a
     * Spring Data derived query method name, so we must keep the {@code @Query}
     * here. Do NOT change this back to a derived method name.
     */
    @Query("""
            SELECT s
            FROM Stock s
            WHERE s.isActive = true
            AND s.currentQuantity <= s.minimumStockLevel
            """)
    List<Stock> findActiveStocksBelowMinimumLevel();


    // =========================================================
    // FIND STOCK WITH PESSIMISTIC WRITE LOCK
    // =========================================================
    /*
     * CRITICAL METHOD FOR CONCURRENCY SAFETY
     *
     * PURCHASE_IN
     * SALE_OUT
     * CANCEL_PURCHASE_OUT
     * CANCEL_SALE_IN
     * ADJUSTMENT_IN
     * ADJUSTMENT_OUT
     *
     * PESSIMISTIC_WRITE prevents concurrent transactions
     * from modifying the same stock record simultaneously.
     */

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM Stock s
            WHERE LOWER(s.rawMaterial) = LOWER(:rawMaterial)
            AND s.unit = :unit
            """)
    Optional<Stock> findStockForUpdate(
            @Param("rawMaterial") String rawMaterial,
            @Param("unit") WeightUnit unit
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM Stock s
            WHERE s.user = :user
            AND LOWER(s.rawMaterial) = LOWER(:rawMaterial)
            AND s.unit = :unit
            """)
    Optional<Stock> findByUserAndRawMaterialForUpdate(
            @Param("user") User user,
            @Param("rawMaterial") String rawMaterial,
            @Param("unit") WeightUnit unit
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM Stock s
            WHERE s.user = :user
            AND LOWER(s.rawMaterial) = LOWER(:rawMaterial)
            """)
    Optional<Stock> findByUserAndRawMaterialIgnoreCaseForUpdate(
            @Param("user") User user,
            @Param("rawMaterial") String rawMaterial
    );


    // =========================================================
    // FIND STOCK BY PUBLIC ID WITH PESSIMISTIC WRITE LOCK
    // =========================================================
    /*
     * Used for manual stock adjustments where the stock
     * is identified using publicId.
     */

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM Stock s
            WHERE s.publicId = :publicId
            """)
    Optional<Stock> findStockForUpdateByPublicId(
            @Param("publicId") UUID publicId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM Stock s
            WHERE s.user = :user
            AND s.publicId = :publicId
            """)
    Optional<Stock> findByUserAndPublicIdForUpdate(
            @Param("user") User user,
            @Param("publicId") UUID publicId
    );
}