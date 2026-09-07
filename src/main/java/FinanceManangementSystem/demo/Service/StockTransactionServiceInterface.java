package FinanceManangementSystem.demo.Service;

import FinanceManangementSystem.demo.Enums.StockTransactionType;
import FinanceManangementSystem.demo.Enums.WeightUnit;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestStockTransactionDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseStockTransactionDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockTransactionServiceInterface {

    // =========================================================
    // CREATE MANUAL ADJUSTMENT
    // =========================================================

    ResponseStockTransactionDTO createAdjustment(
            RequestStockTransactionDTO dto
    );


    // =========================================================
    // GET TRANSACTION BY PUBLIC ID
    // =========================================================

    ResponseStockTransactionDTO getTransactionByPublicId(
            UUID publicId
    );


    // =========================================================
    // GET ALL TRANSACTIONS
    // =========================================================

    default Page<ResponseStockTransactionDTO> getAllTransactions(Pageable pageable) {
        return getAllTransactions(null, null, null, null, null, pageable);
    }

    Page<ResponseStockTransactionDTO> getAllTransactions(
            UUID stockPublicId,
            StockTransactionType type,
            String referenceNumber,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );


    // =========================================================
    // GET TRANSACTIONS BY STOCK
    // =========================================================

    List<ResponseStockTransactionDTO> getTransactionsByStockPublicId(
            UUID stockPublicId
    );


    // =========================================================
    // GET TRANSACTIONS BY STOCK AND TYPE
    // =========================================================

    List<ResponseStockTransactionDTO> getTransactionsByType(
            UUID stockPublicId,
            StockTransactionType transactionType
    );


    // =========================================================
    // GET TRANSACTIONS BY DATE RANGE
    // =========================================================

    List<ResponseStockTransactionDTO> getTransactionsBetweenDates(
            LocalDateTime fromDate,
            LocalDateTime toDate
    );


    // =========================================================
    // GET TRANSACTIONS BY TRANSACTION TYPE
    // =========================================================

    List<ResponseStockTransactionDTO> getTransactionsByTransactionType(
            StockTransactionType transactionType
    );


    // =========================================================
    // GET TRANSACTIONS BY REFERENCE NUMBER
    // =========================================================

    List<ResponseStockTransactionDTO> getTransactionsByReferenceNumber(
            String referenceNumber
    );


    // =========================================================
    // INTERNAL STOCK OPERATIONS
    // =========================================================

    // Purchase completed
    void purchaseStockIn(
            String rawMaterial,
            WeightUnit unit,
            BigDecimal quantity,
            String purchaseNumber
    );

    // Purchase updated
    void updatePurchaseStock(
            FinanceManangementSystem.demo.Model.User user,
            String oldRawMaterial,
            WeightUnit oldUnit,
            BigDecimal oldWeight,
            String newRawMaterial,
            WeightUnit newUnit,
            BigDecimal newWeight,
            String purchaseNumber
    );

    // Purchase reverted (deleted)
    void revertPurchaseStock(
            FinanceManangementSystem.demo.Model.User user,
            String rawMaterial,
            WeightUnit unit,
            BigDecimal quantity,
            String purchaseNumber
    );


    // Sale completed
    void saleStockOut(
            String rawMaterial,
            WeightUnit unit,
            BigDecimal quantity,
            String saleNumber
    );

    // Sale updated
    void updateSaleStock(
            FinanceManangementSystem.demo.Model.User user,
            String rawMaterial,
            WeightUnit unit,
            BigDecimal oldWeight,
            BigDecimal newWeight,
            String saleNumber
    );

    void updateSaleStock(
            FinanceManangementSystem.demo.Model.User user,
            String oldRawMaterial,
            WeightUnit oldUnit,
            BigDecimal oldWeight,
            String newRawMaterial,
            WeightUnit newUnit,
            BigDecimal newWeight,
            String saleNumber
    );

    // Sale reverted (deleted)
    void revertSaleStock(
            FinanceManangementSystem.demo.Model.User user,
            String rawMaterial,
            WeightUnit unit,
            BigDecimal quantity,
            String saleNumber
    );



    // =========================================================
    // MANUAL STOCK ADJUSTMENTS
    // =========================================================

    void adjustmentStockIn(
            UUID stockPublicId,
            BigDecimal quantity,
            String referenceNumber,
            String remarks
    );


    void adjustmentStockOut(
            UUID stockPublicId,
            BigDecimal quantity,
            String referenceNumber,
            String remarks
    );
}