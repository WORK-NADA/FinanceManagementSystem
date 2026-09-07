package FinanceManangementSystem.demo.Repository;

import FinanceManangementSystem.demo.Enums.StockTransactionType;
import FinanceManangementSystem.demo.Model.Stock;
import FinanceManangementSystem.demo.Model.StockTransaction;
import FinanceManangementSystem.demo.Model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;

@Repository
public interface StockTransactionRepository
        extends JpaRepository<StockTransaction, Long>, JpaSpecificationExecutor<StockTransaction> {

    // =========================================================
    // FIND BY PUBLIC ID
    // =========================================================

    Optional<StockTransaction> findByPublicId(
            UUID publicId
    );

    Optional<StockTransaction> findByUserAndPublicId(
            User user,
            UUID publicId
    );


    // =========================================================
    // FIND ALL BY USER
    // =========================================================

    List<StockTransaction> findByUser(
            User user
    );

    Page<StockTransaction> findByUser(
            User user,
            Pageable pageable
    );


    // =========================================================
    // FIND TRANSACTIONS BY STOCK
    // =========================================================

    List<StockTransaction>
    findByStockOrderByTransactionDateDesc(
            Stock stock
    );


    // =========================================================
    // FIND TRANSACTIONS BY STOCK PUBLIC ID
    // =========================================================

    List<StockTransaction>
    findByStockPublicIdOrderByTransactionDateDesc(
            UUID stockPublicId
    );

    List<StockTransaction>
    findByUserAndStockPublicIdOrderByTransactionDateDesc(
            User user,
            UUID stockPublicId
    );


    // =========================================================
    // FIND BY STOCK AND TRANSACTION TYPE
    // =========================================================

    List<StockTransaction>
    findByStockAndTransactionType(
            Stock stock,
            StockTransactionType transactionType
    );


    // =========================================================
    // FIND BY STOCK, TYPE AND DATE RANGE
    // =========================================================

    List<StockTransaction>
    findByStockAndTransactionTypeAndTransactionDateBetween(
            Stock stock,
            StockTransactionType transactionType,
            LocalDateTime fromDate,
            LocalDateTime toDate
    );


    // =========================================================
    // FIND BY DATE RANGE
    // =========================================================

    List<StockTransaction>
    findByTransactionDateBetween(
            LocalDateTime fromDate,
            LocalDateTime toDate
    );

    List<StockTransaction>
    findByUserAndTransactionDateBetween(
            User user,
            LocalDateTime fromDate,
            LocalDateTime toDate
    );


    // =========================================================
    // FIND BY TRANSACTION TYPE
    // =========================================================

    List<StockTransaction>
    findByTransactionType(
            StockTransactionType transactionType
    );

    List<StockTransaction>
    findByUserAndTransactionType(
            User user,
            StockTransactionType transactionType
    );


    // =========================================================
    // FIND BY REFERENCE NUMBER
    // =========================================================

    List<StockTransaction>
    findByReferenceNumber(
            String referenceNumber
    );

    List<StockTransaction>
    findByUserAndReferenceNumber(
            User user,
            String referenceNumber
    );


    // =========================================================
    // CHECK DUPLICATE TRANSACTION
    // =========================================================
    /*
     * The combination of referenceNumber + transactionType
     * identifies one stock transaction operation.
     *
     * Example:
     *
     * PURCHASE_IN + PUR-2026-000001
     * PURCHASE_CANCEL_OUT + PUR-2026-000001
     *
     * These are both allowed because their transaction
     * types are different.
     *
     * But:
     *
     * PURCHASE_IN + PUR-2026-000001
     * PURCHASE_IN + PUR-2026-000001
     *
     * is not allowed.
     */

    boolean existsByUserAndReferenceNumberAndTransactionType(
            User user,
            String referenceNumber,
            StockTransactionType transactionType
    );

    Optional<StockTransaction> findByUserAndReferenceNumberAndTransactionType(
            User user,
            String referenceNumber,
            StockTransactionType transactionType
    );

    void deleteByUserAndReferenceNumberAndTransactionType(
            User user,
            String referenceNumber,
            StockTransactionType transactionType
    );


    // =========================================================
    // LATEST TRANSACTIONS
    // =========================================================

    List<StockTransaction>
    findByOrderByTransactionDateDesc(
            Pageable pageable
    );

    List<StockTransaction>
    findByUserOrderByTransactionDateDesc(
            User user,
            Pageable pageable
    );
}