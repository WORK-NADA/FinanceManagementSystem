package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;

import FinanceManangementSystem.demo.Enums.UserRole;
import FinanceManangementSystem.demo.Exceptions.DuplicateResourceException;
import FinanceManangementSystem.demo.Exceptions.InvalidRequestException;
import FinanceManangementSystem.demo.Exceptions.InvalidStateException;
import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;
import FinanceManangementSystem.demo.Model.Purchase;
import FinanceManangementSystem.demo.Model.Sale;
import FinanceManangementSystem.demo.Model.Stock;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestMinimumStockLevelDTO;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestStockDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseStockDTO;
import FinanceManangementSystem.demo.Repository.PurchaseRepository;
import FinanceManangementSystem.demo.Repository.SaleRepository;
import FinanceManangementSystem.demo.Repository.StockRepository;
import FinanceManangementSystem.demo.Service.StockServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService
        implements StockServiceInterface {

    private final StockRepository stockRepository;

    private final CurrentUserService currentUserService;

    private final PurchaseRepository purchaseRepository;

    private final SaleRepository saleRepository;


    // =========================================================
    // ADD STOCK
    // =========================================================

    @Override
    @Transactional
    public ResponseStockDTO addStock(
            RequestStockDTO dto
    ) {

        log.info(
                "SERVICE - request came in addStock..."
        );


        User currentUser = currentUserService.getCurrentUser();

        String rawMaterial =
                dto.getRawMaterial().trim();


        if (stockRepository
                .existsByUserAndRawMaterialIgnoreCase(
                        currentUser,
                        rawMaterial
                )) {

            log.info(
                    "SERVICE - stock already exists..."
            );

            throw new DuplicateResourceException(
                    "Stock already exists for this raw material"
            );
        }


        Stock stock =
                new Stock();

        stock.setUser(currentUser);

        stock.setRawMaterial(
                rawMaterial
        );

        stock.setUnit(
                dto.getUnit()
        );

        /*
         * Current quantity must always start
         * from zero.
         *
         * Stock quantity will be changed only
         * through StockTransactionService.
         */
        stock.setCurrentQuantity(
                BigDecimal.ZERO
        );

        stock.setMinimumStockLevel(
                dto.getMinimumStockLevel()
        );

        stock.setIsActive(true);


        stock =
                stockRepository.save(
                        stock
                );


        log.info(
                "SERVICE - stock added successfully..."
        );


        return mapToResponse(
                stock
        );
    }


    // =========================================================
    // GET STOCK BY PUBLIC ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ResponseStockDTO getStockByPublicId(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in getStockByPublicId..."
        );


        Stock stock =
                findStock(
                        publicId
                );


        log.info(
                "SERVICE - stock fetched successfully..."
        );


        return mapToResponse(
                stock
        );
    }


    // =========================================================
    // GET ALL ACTIVE STOCKS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseStockDTO> getAllActiveStocks() {

        log.info(
                "SERVICE - request came in getAllActiveStocks..."
        );


        User currentUser = currentUserService.getCurrentUser();

        return stockRepository
                .findByUserAndIsActiveTrue(currentUser)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET ALL INACTIVE STOCKS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseStockDTO> getAllInactiveStocks() {

        log.info(
                "SERVICE - request came in getAllInactiveStocks..."
        );


        User currentUser = currentUserService.getCurrentUser();

        return stockRepository
                .findByUserAndIsActiveFalse(currentUser)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET ALL STOCKS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseStockDTO> getAllStocks() {

        log.info(
                "SERVICE - request came in getAllStocks..."
        );


        User currentUser = currentUserService.getCurrentUser();

        return stockRepository
                .findByUser(currentUser)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // GET LOW STOCK LIST
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseStockDTO> getLowStockList() {

        log.info(
                "SERVICE - request came in getLowStockList..."
        );

        User currentUser = currentUserService.getCurrentUser();

        return stockRepository
                .findByUser(currentUser)
                .stream()
                .filter(stock -> Boolean.TRUE.equals(stock.getIsActive()))
                .filter(stock -> stock.getCurrentQuantity().compareTo(stock.getMinimumStockLevel()) <= 0)
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // SEARCH STOCK (BY RAW MATERIAL OR CURRENT QUANTITY)
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseStockDTO> searchStock(
            String query
    ) {

        log.info(
                "SERVICE - request came in searchStock with query: {}",
                query
        );

        if (query == null ||
                query.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        User currentUser = currentUserService.getCurrentUser();
        String trimmed = query.trim();

        BigDecimal numericVal = null;
        try {
            numericVal = new BigDecimal(trimmed);
        } catch (Exception ignored) {
        }

        List<Stock> allStocks = (currentUser.getRole() == UserRole.ADMIN)
                ? stockRepository.findAll()
                : stockRepository.findByUser(currentUser);

        BigDecimal finalNumeric = numericVal;

        return allStocks.stream()
                .filter(stock -> {
                    boolean matchesName = stock.getRawMaterial() != null
                            && stock.getRawMaterial().toLowerCase().contains(trimmed.toLowerCase());
                    boolean matchesQty = false;
                    if (stock.getCurrentQuantity() != null) {
                        String qtyStr = stock.getCurrentQuantity().stripTrailingZeros().toPlainString();
                        if (qtyStr.contains(trimmed)) {
                            matchesQty = true;
                        } else if (finalNumeric != null && stock.getCurrentQuantity().compareTo(finalNumeric) == 0) {
                            matchesQty = true;
                        }
                    }
                    return matchesName || matchesQty;
                })
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // UPDATE STOCK MASTER DETAILS
    // =========================================================

    @Override
    @Transactional
    public ResponseStockDTO updateStock(
            UUID publicId,
            RequestStockDTO dto
    ) {

        log.info(
                "SERVICE - request came in updateStock..."
        );


        User currentUser = currentUserService.getCurrentUser();

        Stock stock;
        if (currentUser.getRole() == UserRole.ADMIN) {
            stock = stockRepository
                    .findStockForUpdateByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
        } else {
            stock = stockRepository
                    .findByUserAndPublicIdForUpdate(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
        }


        if (!Boolean.TRUE.equals(
                stock.getIsActive()
        )) {

            throw new InvalidStateException(
                    "Cannot update inactive stock"
            );
        }


        String rawMaterial =
                dto.getRawMaterial().trim();


        /*
         * Check whether another stock already
         * exists with the same raw material
         * and unit.
         *
         * The current stock itself must be excluded.
         */
        if (!stock.getRawMaterial().equalsIgnoreCase(rawMaterial)
                && stockRepository.existsByUserAndRawMaterialIgnoreCaseAndPublicIdNot(
                        currentUser,
                        rawMaterial,
                        stock.getPublicId()
                )) {

            throw new DuplicateResourceException(
                    "Stock already exists for this raw material"
            );
        }


        String oldRawMaterial =
                stock.getRawMaterial();

        /*
         * Do not modify currentQuantity or unit here.
         * When the main Edit action is used, it allows
         * editing only Raw Material and Min Level.
         */
        stock.setRawMaterial(
                rawMaterial
        );

        if (dto.getMinimumStockLevel() != null) {
            stock.setMinimumStockLevel(
                    dto.getMinimumStockLevel()
            );
        }

        stock =
                stockRepository.save(
                        stock
                );

        /*
         * When the Raw Material is changed through the Edit functionality,
         * update the Raw Material reference/name across all existing Purchase
         * and Sales records associated with that particular Raw Material so
         * historical records remain correctly linked.
         */
        if (!oldRawMaterial.equalsIgnoreCase(rawMaterial)) {
            log.info(
                    "SERVICE - updating raw material from '{}' to '{}' across historical purchases and sales...",
                    oldRawMaterial,
                    rawMaterial
            );

            List<Purchase> purchases = (currentUser.getRole() == UserRole.ADMIN)
                    ? purchaseRepository.findByRawMaterialIgnoreCase(oldRawMaterial)
                    : purchaseRepository.findByUserAndRawMaterialIgnoreCase(currentUser, oldRawMaterial);
            if (!purchases.isEmpty()) {
                for (Purchase p : purchases) {
                    p.setRawMaterial(rawMaterial);
                }
                purchaseRepository.saveAll(purchases);
                log.info("SERVICE - synchronized {} purchase records to new raw material name '{}'", purchases.size(), rawMaterial);
            }

            List<Sale> sales = (currentUser.getRole() == UserRole.ADMIN)
                    ? saleRepository.findByRawMaterialIgnoreCase(oldRawMaterial)
                    : saleRepository.findByUserAndRawMaterialIgnoreCase(currentUser, oldRawMaterial);
            if (!sales.isEmpty()) {
                for (Sale s : sales) {
                    s.setRawMaterial(rawMaterial);
                }
                saleRepository.saveAll(sales);
                log.info("SERVICE - synchronized {} sale records to new raw material name '{}'", sales.size(), rawMaterial);
            }
        }


        log.info(
                "SERVICE - stock updated successfully..."
        );


        return mapToResponse(
                stock
        );
    }


    // =========================================================
    // UPDATE MINIMUM STOCK LEVEL
    // =========================================================

    @Override
    @Transactional
    public ResponseStockDTO updateMinimumStockLevel(
            UUID publicId,
            RequestMinimumStockLevelDTO dto
    ) {

        log.info(
                "SERVICE - request came in updateMinimumStockLevel..."
        );


        User currentUser = currentUserService.getCurrentUser();

        Stock stock;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            stock = stockRepository.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
        } else {
            stock = stockRepository.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
        }

        if (!Boolean.TRUE.equals(
                stock.getIsActive()
        )) {

            throw new InvalidStateException(
                    "Cannot update minimum stock level for inactive stock"
            );
        }


        stock.setMinimumStockLevel(
                dto.getMinimumStockLevel()
        );


        stock =
                stockRepository.save(
                        stock
                );


        log.info(
                "SERVICE - minimum stock level updated successfully..."
        );


        return mapToResponse(
                stock
        );
    }


    // =========================================================
    // ACTIVATE STOCK
    // =========================================================

    @Override
    @Transactional
    public void activateStock(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in activateStock..."
        );


        User currentUser = currentUserService.getCurrentUser();

        Stock stock;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            stock = stockRepository.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
        } else {
            stock = stockRepository.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
        }

        if (Boolean.TRUE.equals(
                stock.getIsActive()
        )) {

            throw new InvalidStateException(
                    "Stock is already active"
            );
        }


        stock.setIsActive(true);


        stockRepository.save(
                stock
        );


        log.info(
                "SERVICE - stock activated successfully..."
        );
    }


    // =========================================================
    // DEACTIVATE STOCK
    // =========================================================

    @Override
    @Transactional
    public void deactivateStock(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in deactivateStock..."
        );


        User currentUser = currentUserService.getCurrentUser();

        Stock stock;
        if (currentUser.getRole() == UserRole.ADMIN) {
            stock = stockRepository
                    .findStockForUpdateByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
        } else {
            stock = stockRepository
                    .findByUserAndPublicIdForUpdate(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
        }

        if (!Boolean.TRUE.equals(
                stock.getIsActive()
        )) {

            throw new InvalidStateException(
                    "Stock is already inactive"
            );
        }

        stock.setIsActive(false);

        stockRepository.save(
                stock
        );

        log.info(
                "SERVICE - stock deactivated successfully..."
        );
    }


    // =========================================================
    // FIND STOCK
    // =========================================================

    private Stock findStock(
            UUID publicId
    ) {

        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == UserRole.ADMIN) {
            return stockRepository.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
        }

        return stockRepository
                .findByUserAndPublicId(
                        currentUser,
                        publicId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock not found"
                        )
                );
    }


    // =========================================================
    // MAP TO RESPONSE
    // =========================================================

    private ResponseStockDTO mapToResponse(
            Stock stock
    ) {

        ResponseStockDTO response =
                new ResponseStockDTO();


        response.setPublicId(
                stock.getPublicId()
        );

        response.setRawMaterial(
                stock.getRawMaterial()
        );

        response.setUnit(
                stock.getUnit()
        );

        response.setCurrentQuantity(
                stock.getCurrentQuantity()
        );

        response.setMinimumStockLevel(
                stock.getMinimumStockLevel()
        );

        response.setIsLowStock(
                stock.getCurrentQuantity()
                        .compareTo(
                                stock.getMinimumStockLevel()
                        ) <= 0
        );

        response.setIsActive(
                stock.getIsActive()
        );

        response.setCreatedAt(
                stock.getCreatedAt()
        );

        response.setUpdatedAt(
                stock.getUpdatedAt()
        );


        return response;
    }
}