package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;

import FinanceManangementSystem.demo.Exceptions.DuplicateResourceException;
import FinanceManangementSystem.demo.Exceptions.InvalidRequestException;
import FinanceManangementSystem.demo.Exceptions.InvalidStateException;
import FinanceManangementSystem.demo.Exceptions.InsufficientStockException;
import FinanceManangementSystem.demo.Enums.DocumentType;
import FinanceManangementSystem.demo.Enums.StockTransactionType;
import FinanceManangementSystem.demo.Enums.WeightUnit;
import FinanceManangementSystem.demo.Model.Stock;
import FinanceManangementSystem.demo.Model.StockTransaction;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestStockTransactionDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseStockTransactionDTO;
import FinanceManangementSystem.demo.Repository.StockRepository;
import FinanceManangementSystem.demo.Repository.StockTransactionRepository;
import FinanceManangementSystem.demo.Service.StockTransactionServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import FinanceManangementSystem.demo.Specification.StockTransactionSpecification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockTransactionService
        implements StockTransactionServiceInterface {

    private final CurrentUserService currentUserService;

    private final StockRepository stockRepository;

    private final StockTransactionRepository stockTransactionRepository;

    private final DocumentSequenceService documentSequenceService;


    // =========================================================
    // CREATE MANUAL ADJUSTMENT
    // =========================================================

    @Override
    @Transactional
    public ResponseStockTransactionDTO createAdjustment(
            RequestStockTransactionDTO dto
    ) {

        log.info(
                "SERVICE - request came in createAdjustment..."
        );


        if (dto.getTransactionType()
                != StockTransactionType.ADJUSTMENT_IN
                &&
                dto.getTransactionType()
                        != StockTransactionType.ADJUSTMENT_OUT) {

            throw new InvalidRequestException(
                    "Only adjustment transactions are allowed"
            );
        }


        validateQuantity(
                dto.getQuantity()
        );


        User currentUser = currentUserService.getCurrentUser();

        Stock stock =
                stockRepository
                        .findByUserAndPublicId(currentUser, dto.getStockPublicId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Stock not found"
                                )
                        );


        validateActiveStock(
                stock
        );


        if (dto.getTransactionType()
                == StockTransactionType.ADJUSTMENT_IN) {

            increaseStock(
                    stock,
                    dto.getQuantity()
            );

        } else {

            decreaseStock(
                    stock,
                    dto.getQuantity()
            );
        }


        String referenceNumber =
                documentSequenceService
                        .generateDocumentNumber(
                                DocumentType.STOCK_ADJUSTMENT,
                                LocalDateTime.now().getYear()
                        );


        StockTransaction transaction =
                createTransaction(
                        currentUser,
                        stock,
                        dto.getTransactionType(),
                        dto.getQuantity(),
                        referenceNumber,
                        dto.getRemarks()
                );


        log.info(
                "SERVICE - stock adjustment created successfully..."
        );


        return mapToResponse(
                transaction
        );
    }


    // =========================================================
    // PURCHASE IN
    // =========================================================

    // =========================================================
    // UNIT CONVERSION HELPER
    // =========================================================

    public static BigDecimal convertQuantity(
            BigDecimal quantity,
            WeightUnit fromUnit,
            WeightUnit toUnit
    ) {

        if (quantity == null) {
            return BigDecimal.ZERO;
        }

        if (fromUnit == null || toUnit == null || fromUnit == toUnit) {
            return quantity;
        }

        // 1. Convert fromUnit to KG (base unit)
        BigDecimal inKg;
        switch (fromUnit) {
            case G -> inKg = quantity.divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
            case TON -> inKg = quantity.multiply(BigDecimal.valueOf(1000));
            case KG -> inKg = quantity;
            default -> inKg = quantity;
        }

        // 2. Convert from KG to toUnit
        BigDecimal result;
        switch (toUnit) {
            case G -> result = inKg.multiply(BigDecimal.valueOf(1000));
            case TON -> result = inKg.divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
            case KG -> result = inKg;
            default -> result = inKg;
        }

        return result.setScale(3, RoundingMode.HALF_UP);
    }


    // =========================================================
    // PURCHASE IN
    // =========================================================

    @Override
    @Transactional
    public void purchaseStockIn(
            String rawMaterial,
            WeightUnit unit,
            BigDecimal quantity,
            String purchaseNumber
    ) {

        log.info(
                "SERVICE - request came in purchaseStockIn..."
        );

        validateQuantity(
                quantity
        );

        if (rawMaterial == null || rawMaterial.trim().isEmpty()) {
            throw new InvalidRequestException(
                    "Raw material is required"
            );
        }

        User currentUser = currentUserService.getCurrentUser();
        String trimmedRawMaterial = rawMaterial.trim();
        WeightUnit effectiveUnit = unit != null ? unit : WeightUnit.KG;

        // Find existing stock record ignoring case sensitivity for this client
        Stock stock = stockRepository
                .findByUserAndRawMaterialIgnoreCaseForUpdate(
                        currentUser,
                        trimmedRawMaterial
                )
                .orElseGet(() -> {
                    log.info(
                            "SERVICE - stock not found for client, automatically creating new stock master for: {} ({})",
                            trimmedRawMaterial,
                            effectiveUnit
                    );
                    Stock newStock = new Stock();
                    newStock.setUser(currentUser);
                    newStock.setRawMaterial(trimmedRawMaterial);
                    newStock.setUnit(effectiveUnit);
                    newStock.setCurrentQuantity(BigDecimal.ZERO);
                    newStock.setMinimumStockLevel(BigDecimal.ZERO);
                    newStock.setIsActive(true);
                    return stockRepository.save(newStock);
                });

        if (!Boolean.TRUE.equals(stock.getIsActive())) {
            stock.setIsActive(true);
        }

        // Convert incoming purchased quantity to stock's canonical unit if different
        BigDecimal convertedQuantity = convertQuantity(quantity, effectiveUnit, stock.getUnit());

        increaseStock(
                stock,
                convertedQuantity
        );

        createTransaction(
                currentUser,
                stock,
                StockTransactionType.PURCHASE_IN,
                convertedQuantity,
                purchaseNumber,
                "Stock added through purchase" + (effectiveUnit != stock.getUnit() ? " (" + quantity + " " + effectiveUnit + ")" : "")
        );

        log.info(
                "SERVICE - purchase stock added successfully to single record..."
        );
    }


    // =========================================================
    // UPDATE PURCHASE STOCK
    // =========================================================

    @Override
    @Transactional
    public void updatePurchaseStock(
            User user,
            String oldRawMaterial,
            WeightUnit oldUnit,
            BigDecimal oldWeight,
            String newRawMaterial,
            WeightUnit newUnit,
            BigDecimal newWeight,
            String purchaseNumber
    ) {
        log.info("SERVICE - updating purchase stock for purchase: {}", purchaseNumber);
        validateQuantity(oldWeight);
        validateQuantity(newWeight);

        if (newRawMaterial == null || newRawMaterial.trim().isEmpty()) {
            throw new InvalidRequestException("Raw material is required");
        }

        String trimmedOld = oldRawMaterial.trim();
        String trimmedNew = newRawMaterial.trim();
        WeightUnit effOldUnit = oldUnit != null ? oldUnit : WeightUnit.KG;
        WeightUnit effNewUnit = newUnit != null ? newUnit : WeightUnit.KG;

        boolean sameMaterial = trimmedOld.equalsIgnoreCase(trimmedNew);

        if (sameMaterial) {
            Stock stock = stockRepository
                    .findByUserAndRawMaterialIgnoreCaseForUpdate(user, trimmedOld)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found for: " + trimmedOld));

            BigDecimal oldConverted = convertQuantity(oldWeight, effOldUnit, stock.getUnit());
            BigDecimal newConverted = convertQuantity(newWeight, effNewUnit, stock.getUnit());
            BigDecimal diff = newConverted.subtract(oldConverted);

            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                increaseStock(stock, diff);
            } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                BigDecimal absDiff = diff.abs();
                if (stock.getCurrentQuantity().compareTo(absDiff) < 0) {
                    throw new InvalidRequestException(
                            "Cannot reduce purchase quantity. Available stock is " +
                                    stock.getCurrentQuantity() + " " + stock.getUnit() +
                                    ", which is less than the required reduction of " +
                                    absDiff + " " + stock.getUnit() + "."
                    );
                }
                decreaseStock(stock, absDiff);
            }

            stockTransactionRepository.findByUserAndReferenceNumberAndTransactionType(
                    user, purchaseNumber.trim(), StockTransactionType.PURCHASE_IN
            ).ifPresent(tx -> {
                tx.setQuantity(newConverted);
                tx.setRemarks("Stock added through purchase (updated)" +
                        (effNewUnit != stock.getUnit() ? " (" + newWeight + " " + effNewUnit + ")" : ""));
                stockTransactionRepository.save(tx);
            });
        } else {
            // Material changed: revert old stock, add to new stock
            revertPurchaseStock(user, trimmedOld, effOldUnit, oldWeight, purchaseNumber);

            Stock newStock = stockRepository
                    .findByUserAndRawMaterialIgnoreCaseForUpdate(user, trimmedNew)
                    .orElseGet(() -> {
                        Stock s = new Stock();
                        s.setUser(user);
                        s.setRawMaterial(trimmedNew);
                        s.setUnit(effNewUnit);
                        s.setCurrentQuantity(BigDecimal.ZERO);
                        s.setMinimumStockLevel(BigDecimal.ZERO);
                        s.setIsActive(true);
                        return stockRepository.save(s);
                    });

            if (!Boolean.TRUE.equals(newStock.getIsActive())) {
                newStock.setIsActive(true);
            }

            BigDecimal newConverted = convertQuantity(newWeight, effNewUnit, newStock.getUnit());
            increaseStock(newStock, newConverted);

            createTransaction(
                    user,
                    newStock,
                    StockTransactionType.PURCHASE_IN,
                    newConverted,
                    purchaseNumber.trim(),
                    "Stock added through purchase (material changed from " + trimmedOld + ")"
            );
        }
        log.info("SERVICE - purchase stock updated successfully for: {}", purchaseNumber);
    }


    // =========================================================
    // REVERT PURCHASE STOCK (DELETION)
    // =========================================================

    @Override
    @Transactional
    public void revertPurchaseStock(
            User user,
            String rawMaterial,
            WeightUnit unit,
            BigDecimal quantity,
            String purchaseNumber
    ) {
        log.info("SERVICE - reverting purchase stock for purchase: {}", purchaseNumber);
        validateQuantity(quantity);
        if (rawMaterial == null || rawMaterial.trim().isEmpty()) {
            throw new InvalidRequestException("Raw material is required");
        }

        String trimmedRawMaterial = rawMaterial.trim();
        WeightUnit effectiveUnit = unit != null ? unit : WeightUnit.KG;

        Stock stock = stockRepository
                .findByUserAndRawMaterialIgnoreCaseForUpdate(user, trimmedRawMaterial)
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for: " + trimmedRawMaterial));

        BigDecimal convertedQuantity = convertQuantity(quantity, effectiveUnit, stock.getUnit());

        if (stock.getCurrentQuantity().compareTo(convertedQuantity) < 0) {
            throw new InvalidRequestException(
                    "Cannot delete purchase #" + purchaseNumber +
                            ". Available stock for " + stock.getRawMaterial() + " is " +
                            stock.getCurrentQuantity() + " " + stock.getUnit() +
                            ", which is less than the purchased quantity (" +
                            convertedQuantity + " " + stock.getUnit() + "). " +
                            "The stock from this purchase has already been consumed by sales or adjustments."
            );
        }

        decreaseStock(stock, convertedQuantity);

        // Record "Cancel Purchase" stock history entry instead of deleting the original PURCHASE_IN entry
        createTransaction(
                user,
                stock,
                StockTransactionType.CANCEL_PURCHASE_OUT,
                convertedQuantity,
                purchaseNumber.trim(),
                "Stock reversed due to deletion/cancellation of Purchase #" + purchaseNumber.trim()
        );
        log.info("SERVICE - purchase stock cancelled and reversal recorded for purchase: {}", purchaseNumber);
    }


    // =========================================================
    // SALE OUT
    // =========================================================

    @Override
    @Transactional
    public void saleStockOut(
            String rawMaterial,
            WeightUnit unit,
            BigDecimal quantity,
            String saleNumber
    ) {

        log.info(
                "SERVICE - request came in saleStockOut..."
        );

        validateQuantity(
                quantity
        );

        if (rawMaterial == null || rawMaterial.trim().isEmpty()) {
            throw new InvalidRequestException(
                    "Raw material is required"
            );
        }

        User currentUser = currentUserService.getCurrentUser();
        String trimmedRawMaterial = rawMaterial.trim();
        WeightUnit effectiveUnit = unit != null ? unit : WeightUnit.KG;

        // Find existing stock record ignoring case sensitivity for this client
        Stock stock = stockRepository
                .findByUserAndRawMaterialIgnoreCaseForUpdate(
                        currentUser,
                        trimmedRawMaterial
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock not found for raw material: " + trimmedRawMaterial
                        )
                );

        validateActiveStock(
                stock
        );

        // Convert sale quantity to stock's canonical unit if different
        BigDecimal convertedQuantity = convertQuantity(quantity, effectiveUnit, stock.getUnit());

        decreaseStock(
                stock,
                convertedQuantity
        );

        createTransaction(
                currentUser,
                stock,
                StockTransactionType.SALE_OUT,
                convertedQuantity,
                saleNumber,
                "Stock removed through sale" + (effectiveUnit != stock.getUnit() ? " (" + quantity + " " + effectiveUnit + ")" : "")
        );

        log.info(
                "SERVICE - sale stock deducted successfully from single record..."
        );
    }


    // =========================================================
    // UPDATE SALE STOCK (EDIT)
    // =========================================================

    @Override
    @Transactional
    public void updateSaleStock(
            User user,
            String rawMaterial,
            WeightUnit unit,
            BigDecimal oldWeight,
            BigDecimal newWeight,
            String saleNumber
    ) {
        log.info("SERVICE - updating sale stock for sale: {}", saleNumber);
        validateQuantity(newWeight);
        if (rawMaterial == null || rawMaterial.trim().isEmpty()) {
            throw new InvalidRequestException("Raw material is required");
        }

        String trimmedRawMaterial = rawMaterial.trim();
        WeightUnit effectiveUnit = unit != null ? unit : WeightUnit.KG;

        Stock stock = stockRepository
                .findByUserAndRawMaterialIgnoreCaseForUpdate(user, trimmedRawMaterial)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found for raw material: " + trimmedRawMaterial));

        validateActiveStock(stock);

        BigDecimal oldConverted = convertQuantity(oldWeight, effectiveUnit, stock.getUnit());
        BigDecimal newConverted = convertQuantity(newWeight, effectiveUnit, stock.getUnit());
        BigDecimal diff = newConverted.subtract(oldConverted);

        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            // Increasing sale quantity -> need to deduct more stock from inventory
            if (stock.getCurrentQuantity().compareTo(diff) < 0) {
                throw new InvalidRequestException(
                        "Cannot increase sale quantity. Available stock for " + stock.getRawMaterial() + " is " +
                                stock.getCurrentQuantity() + " " + stock.getUnit() +
                                ", but additional " + diff + " " + stock.getUnit() + " is required."
                );
            }
            decreaseStock(stock, diff);
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            // Decreasing sale quantity -> return the difference back to inventory
            BigDecimal returnedQty = diff.abs();
            increaseStock(stock, returnedQty);
        }

        stockTransactionRepository.findByUserAndReferenceNumberAndTransactionType(
                user, saleNumber.trim(), StockTransactionType.SALE_OUT
        ).ifPresent(tx -> {
            tx.setQuantity(newConverted);
            tx.setRemarks("Stock removed through sale (updated)" +
                    (effectiveUnit != stock.getUnit() ? " (" + newWeight + " " + effectiveUnit + ")" : ""));
            stockTransactionRepository.save(tx);
        });

        log.info("SERVICE - sale stock updated successfully for: {}", saleNumber);
    }


    @Override
    @Transactional
    public void updateSaleStock(
            User user,
            String oldRawMaterial,
            WeightUnit oldUnit,
            BigDecimal oldWeight,
            String newRawMaterial,
            WeightUnit newUnit,
            BigDecimal newWeight,
            String saleNumber
    ) {
        log.info("SERVICE - updating sale stock for sale with material change: {}", saleNumber);
        validateQuantity(newWeight);
        if (newRawMaterial == null || newRawMaterial.trim().isEmpty()) {
            throw new InvalidRequestException("Raw material is required");
        }

        String trimmedOld = oldRawMaterial != null ? oldRawMaterial.trim() : "";
        String trimmedNew = newRawMaterial.trim();
        WeightUnit effOldUnit = oldUnit != null ? oldUnit : WeightUnit.KG;
        WeightUnit effNewUnit = newUnit != null ? newUnit : WeightUnit.KG;

        boolean sameMaterial = trimmedOld.equalsIgnoreCase(trimmedNew) && effOldUnit == effNewUnit;

        if (sameMaterial) {
            updateSaleStock(user, trimmedOld, effOldUnit, oldWeight, newWeight, saleNumber);
            return;
        }

        // Material changed:
        // 1. Return old stock
        if (!trimmedOld.isEmpty() && oldWeight != null && oldWeight.compareTo(BigDecimal.ZERO) > 0) {
            Stock oldStock = stockRepository
                    .findByUserAndRawMaterialIgnoreCaseForUpdate(user, trimmedOld)
                    .orElse(null);

            if (oldStock != null) {
                BigDecimal oldConverted = convertQuantity(oldWeight, effOldUnit, oldStock.getUnit());
                increaseStock(oldStock, oldConverted);
            }
        }

        // 2. Check and deduct new stock
        Stock newStock = stockRepository
                .findByUserAndRawMaterialIgnoreCaseForUpdate(user, trimmedNew)
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for raw material: " + trimmedNew));

        validateActiveStock(newStock);

        BigDecimal newConverted = convertQuantity(newWeight, effNewUnit, newStock.getUnit());
        if (newStock.getCurrentQuantity().compareTo(newConverted) < 0) {
            throw new InvalidRequestException(
                    "Cannot update sale material to " + newStock.getRawMaterial() +
                            ". Available stock is " + newStock.getCurrentQuantity() + " " + newStock.getUnit() +
                            ", but required quantity is " + newConverted + " " + newStock.getUnit() + "."
            );
        }
        decreaseStock(newStock, newConverted);

        // 3. Update stock transaction record
        stockTransactionRepository.findByUserAndReferenceNumberAndTransactionType(
                user, saleNumber.trim(), StockTransactionType.SALE_OUT
        ).ifPresent(tx -> {
            tx.setStock(newStock);
            tx.setQuantity(newConverted);
            tx.setRemarks("Stock removed through sale (material changed from " + trimmedOld + ")" +
                    (effNewUnit != newStock.getUnit() ? " (" + newWeight + " " + effNewUnit + ")" : ""));
            stockTransactionRepository.save(tx);
        });

        log.info("SERVICE - sale stock updated with material change successfully for: {}", saleNumber);
    }


    // =========================================================
    // REVERT SALE STOCK (DELETION)
    // =========================================================

    @Override
    @Transactional
    public void revertSaleStock(
            User user,
            String rawMaterial,
            WeightUnit unit,
            BigDecimal quantity,
            String saleNumber
    ) {
        log.info("SERVICE - reverting sale stock for sale: {}", saleNumber);
        validateQuantity(quantity);
        if (rawMaterial == null || rawMaterial.trim().isEmpty()) {
            throw new InvalidRequestException("Raw material is required");
        }

        String trimmedRawMaterial = rawMaterial.trim();
        WeightUnit effectiveUnit = unit != null ? unit : WeightUnit.KG;

        Stock stock = stockRepository
                .findByUserAndRawMaterialIgnoreCaseForUpdate(user, trimmedRawMaterial)
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for: " + trimmedRawMaterial));

        BigDecimal convertedQuantity = convertQuantity(quantity, effectiveUnit, stock.getUnit());

        // Return sold quantity back to available inventory
        increaseStock(stock, convertedQuantity);

        // Record "Cancel Sale" stock history entry instead of deleting the original SALE_OUT entry
        createTransaction(
                user,
                stock,
                StockTransactionType.CANCEL_SALE_IN,
                convertedQuantity,
                saleNumber.trim(),
                "Stock returned to inventory due to deletion/cancellation of Sale #" + saleNumber.trim()
        );
        log.info("SERVICE - sale stock cancelled and reversal recorded for sale: {}", saleNumber);
    }


    // =========================================================
    // ADJUSTMENT IN
    // =========================================================

    @Override
    @Transactional
    public void adjustmentStockIn(
            UUID stockPublicId,
            BigDecimal quantity,
            String referenceNumber,
            String remarks
    ) {

        log.info(
                "SERVICE - request came in adjustmentStockIn..."
        );


        validateQuantity(
                quantity
        );

        User currentUser = currentUserService.getCurrentUser();

        Stock stock =
                stockRepository
                        .findByUserAndPublicIdForUpdate(
                                currentUser,
                                stockPublicId
                        )
                        .orElseThrow(() ->
                                new InvalidRequestException(
                                        "Stock not found"
                                )
                        );


        validateActiveStock(
                stock
        );


        increaseStock(
                stock,
                quantity
        );


        createTransaction(
                currentUser,
                stock,
                StockTransactionType.ADJUSTMENT_IN,
                quantity,
                referenceNumber,
                remarks
        );


        log.info(
                "SERVICE - stock adjustment IN completed successfully..."
        );
    }


    // =========================================================
    // ADJUSTMENT OUT
    // =========================================================

    @Override
    @Transactional
    public void adjustmentStockOut(
            UUID stockPublicId,
            BigDecimal quantity,
            String referenceNumber,
            String remarks
    ) {

        log.info(
                "SERVICE - request came in adjustmentStockOut..."
        );


        validateQuantity(
                quantity
        );

        User currentUser = currentUserService.getCurrentUser();

        Stock stock =
                stockRepository
                        .findByUserAndPublicIdForUpdate(
                                currentUser,
                                stockPublicId
                        )
                        .orElseThrow(() ->
                                new InvalidRequestException(
                                        "Stock not found"
                                )
                        );


        validateActiveStock(
                stock
        );


        decreaseStock(
                stock,
                quantity
        );


        createTransaction(
                currentUser,
                stock,
                StockTransactionType.ADJUSTMENT_OUT,
                quantity,
                referenceNumber,
                remarks
        );


        log.info(
                "SERVICE - stock adjustment OUT completed successfully..."
        );
    }


    // =========================================================
    // GET TRANSACTION BY PUBLIC ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ResponseStockTransactionDTO getTransactionByPublicId(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in getTransactionByPublicId..."
        );


        User currentUser = currentUserService.getCurrentUser();

        StockTransaction transaction;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            transaction = stockTransactionRepository
                    .findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock transaction not found"));
        } else {
            transaction = stockTransactionRepository
                    .findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock transaction not found"));
        }

        return mapToResponse(transaction);
    }


    // =========================================================
    // GET ALL TRANSACTIONS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseStockTransactionDTO> getAllTransactions(@NonNull Pageable pageable) {
        return getAllTransactions(null, null, null, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseStockTransactionDTO> getAllTransactions(
            UUID stockPublicId,
            StockTransactionType type,
            String referenceNumber,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        log.info("SERVICE - request came in getAllTransactions with filters...");
        User currentUser = currentUserService.getCurrentUser();
        User filterUser = (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) ? null : currentUser;

        Specification<StockTransaction> spec = StockTransactionSpecification.filter(
                filterUser,
                stockPublicId,
                type,
                referenceNumber,
                fromDate,
                toDate
        );

        return stockTransactionRepository.findAll(spec, pageable).map(this::mapToResponse);
    }


    // =========================================================
    // GET STOCK HISTORY
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseStockTransactionDTO>
    getTransactionsByStockPublicId(
            UUID stockPublicId
    ) {

        log.info(
                "SERVICE - request came in getTransactionsByStockPublicId..."
        );


        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {

            stockRepository.findByPublicId(stockPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));

            return stockTransactionRepository
                    .findByStockPublicIdOrderByTransactionDateDesc(stockPublicId)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();

        } else {

            stockRepository.findByUserAndPublicId(currentUser, stockPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));

            return stockTransactionRepository
                    .findByUserAndStockPublicIdOrderByTransactionDateDesc(currentUser, stockPublicId)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }
    }


    // =========================================================
    // GET BY STOCK AND TYPE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseStockTransactionDTO> getTransactionsByType(
            UUID stockPublicId,
            StockTransactionType transactionType
    ) {

        log.info(
                "SERVICE - request came in getTransactionsByType..."
        );


        User currentUser = currentUserService.getCurrentUser();

        Stock stock;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            stock = stockRepository.findByPublicId(stockPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
        } else {
            stock = stockRepository.findByUserAndPublicId(currentUser, stockPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
        }

        if (transactionType == null) {

            throw new InvalidRequestException(
                    "Transaction type is required"
            );
        }


        return stockTransactionRepository
                .findByStockAndTransactionType(
                        stock,
                        transactionType
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET BETWEEN DATES
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseStockTransactionDTO>
    getTransactionsBetweenDates(
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {

        log.info(
                "SERVICE - request came in getTransactionsBetweenDates..."
        );


        if (fromDate == null ||
                toDate == null) {

            throw new InvalidRequestException(
                    "From date and to date are required"
            );
        }


        if (fromDate.isAfter(toDate)) {
            return java.util.Collections.emptyList();
        }


        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            return stockTransactionRepository
                    .findByTransactionDateBetween(fromDate, toDate)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        } else {
            return stockTransactionRepository
                    .findByUserAndTransactionDateBetween(currentUser, fromDate, toDate)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }
    }


    // =========================================================
    // GET BY TRANSACTION TYPE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseStockTransactionDTO>
    getTransactionsByTransactionType(
            StockTransactionType transactionType
    ) {

        log.info(
                "SERVICE - request came in getTransactionsByTransactionType..."
        );


        if (transactionType == null) {

            throw new InvalidRequestException(
                    "Transaction type is required"
            );
        }


        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            return stockTransactionRepository
                    .findByTransactionType(transactionType)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        } else {
            return stockTransactionRepository
                    .findByUserAndTransactionType(currentUser, transactionType)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }
    }


    // =========================================================
    // GET BY REFERENCE NUMBER
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseStockTransactionDTO>
    getTransactionsByReferenceNumber(
            String referenceNumber
    ) {

        log.info(
                "SERVICE - request came in getTransactionsByReferenceNumber..."
        );


        if (referenceNumber == null ||
                referenceNumber.trim().isEmpty()) {

            throw new InvalidRequestException(
                    "Reference number is required"
            );
        }


        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            return stockTransactionRepository
                    .findByReferenceNumber(referenceNumber.trim())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        } else {
            return stockTransactionRepository
                    .findByUserAndReferenceNumber(currentUser, referenceNumber.trim())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }
    }


    // =========================================================
    // FIND STOCK WITH LOCK
    // =========================================================

    private Stock findStockForUpdate(
            String rawMaterial,
            WeightUnit unit
    ) {

        if (rawMaterial == null ||
                rawMaterial.trim().isEmpty()) {

            throw new InvalidRequestException(
                    "Raw material is required"
            );
        }

        User currentUser = currentUserService.getCurrentUser();

        return stockRepository
                .findByUserAndRawMaterialIgnoreCaseForUpdate(
                        currentUser,
                        rawMaterial.trim()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock not found for raw material: " + rawMaterial.trim()
                        )
                );
    }


    // =========================================================
    // INCREASE STOCK
    // =========================================================

    private void increaseStock(
            Stock stock,
            BigDecimal quantity
    ) {

        stock.setCurrentQuantity(
                stock.getCurrentQuantity()
                        .add(quantity)
        );


        stockRepository.save(
                stock
        );
    }


    // =========================================================
    // DECREASE STOCK
    // =========================================================

    private void decreaseStock(
            Stock stock,
            BigDecimal quantity
    ) {

        if (stock.getCurrentQuantity()
                .compareTo(quantity) < 0) {

            throw new InsufficientStockException(
                    "Insufficient stock. Available: "
                            + stock.getCurrentQuantity()
                            + " "
                            + stock.getUnit()
                            + ", Requested: "
                            + quantity
                            + " "
                            + stock.getUnit()
            );
        }


        stock.setCurrentQuantity(
                stock.getCurrentQuantity()
                        .subtract(quantity)
        );


        stockRepository.save(
                stock
        );
    }


    // =========================================================
    // VALIDATE ACTIVE STOCK
    // =========================================================

    private void validateActiveStock(
            Stock stock
    ) {

        if (!Boolean.TRUE.equals(
                stock.getIsActive()
        )) {

            throw new InvalidStateException(
                    "Stock is inactive"
            );
        }
    }


    // =========================================================
    // VALIDATE QUANTITY
    // =========================================================

    private void validateQuantity(
            BigDecimal quantity
    ) {

        if (quantity == null ||
                quantity.compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            throw new InvalidRequestException(
                    "Quantity must be greater than zero"
            );
        }
    }


    // =========================================================
    // CREATE TRANSACTION
    // =========================================================

    private StockTransaction createTransaction(
            User user,
            Stock stock,
            StockTransactionType transactionType,
            BigDecimal quantity,
            String referenceNumber,
            String remarks
    ) {

        if (referenceNumber == null ||
                referenceNumber.trim().isEmpty()) {

            throw new InvalidRequestException(
                    "Reference number is required"
            );
        }


        String trimmedReference =
                referenceNumber.trim();


        if (stockTransactionRepository
                .existsByUserAndReferenceNumberAndTransactionType(
                        user,
                        trimmedReference,
                        transactionType
                )) {

            throw new DuplicateResourceException(
                    "Stock transaction already exists for this reference"
            );
        }


        StockTransaction transaction =
                new StockTransaction();


        transaction.setUser(
                user
        );

        transaction.setStock(
                stock
        );

        transaction.setTransactionType(
                transactionType
        );

        transaction.setQuantity(
                quantity
        );

        /*
         * Unit is always taken from Stock.
         * This prevents a transaction from
         * having a different unit from Stock.
         */

        transaction.setUnit(
                stock.getUnit()
        );

        transaction.setReferenceNumber(
                trimmedReference
        );

        transaction.setTransactionDate(
                LocalDateTime.now()
        );

        transaction.setRemarks(
                remarks
        );


        return stockTransactionRepository.save(
                transaction
        );
    }


    // =========================================================
    // MAP TO RESPONSE
    // =========================================================

    private ResponseStockTransactionDTO mapToResponse(
            StockTransaction transaction
    ) {

        ResponseStockTransactionDTO response =
                new ResponseStockTransactionDTO();


        response.setPublicId(
                transaction.getPublicId()
        );

        response.setStockPublicId(
                transaction.getStock()
                        .getPublicId()
        );

        response.setRawMaterial(
                transaction.getStock()
                        .getRawMaterial()
        );

        response.setTransactionType(
                transaction.getTransactionType()
        );

        response.setQuantity(
                transaction.getQuantity()
        );

        response.setUnit(
                transaction.getUnit()
        );

        response.setReferenceNumber(
                transaction.getReferenceNumber()
        );

        response.setTransactionDate(
                transaction.getTransactionDate()
        );

        response.setRemarks(
                transaction.getRemarks()
        );

        response.setCreatedAt(
                transaction.getCreatedAt()
        );


        return response;
    }
}
