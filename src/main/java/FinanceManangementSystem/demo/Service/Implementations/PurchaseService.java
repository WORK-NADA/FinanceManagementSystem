package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Exceptions.InvalidRequestException;
import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;

import FinanceManangementSystem.demo.Enums.DocumentType;
import FinanceManangementSystem.demo.Enums.PaymentStatus;
import FinanceManangementSystem.demo.Enums.PurchaseStatus;
import FinanceManangementSystem.demo.Enums.UserRole;
import FinanceManangementSystem.demo.Enums.WeightUnit;
import FinanceManangementSystem.demo.Model.Purchase;
import FinanceManangementSystem.demo.Model.Supplier;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestPurchaseDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponsePurchaseDTO;
import FinanceManangementSystem.demo.Repository.PurchasePaymentRepository;
import FinanceManangementSystem.demo.Repository.PurchaseRepository;
import FinanceManangementSystem.demo.Repository.SupplierRepository;
import FinanceManangementSystem.demo.Service.PurchaseServiceInterface;
import FinanceManangementSystem.demo.Service.StockTransactionServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import FinanceManangementSystem.demo.Specification.PurchaseSpecification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService
        implements PurchaseServiceInterface {

    private final PurchaseRepository purchaseRepo;

    private final SupplierRepository supplierRepo;

    private final PurchasePaymentRepository purchasePaymentRepo;

    private final CurrentUserService currentUserService;

    private final DocumentSequenceService documentSequenceService;

    private final ModelMapper modelMapper;

    private final StockTransactionServiceInterface
            stockTransactionService;


    // =========================================================
    // ADD PURCHASE
    // =========================================================

    @Override
    @Transactional
    public ResponsePurchaseDTO addPurchase(
            RequestPurchaseDTO dto
    ) {

        log.info(
                "SERVICE - request came in addPurchase..."
        );


        // -----------------------------------------------------
        // FIND ACTIVE SUPPLIER
        // -----------------------------------------------------

        log.info(
                "SERVICE - finding active supplier..."
        );

        Supplier supplier =
                supplierRepo
                        .findByPublicIdAndIsActiveTrue(
                                dto.getSupplierPublicId()
                        )
                        .orElseThrow(() -> {

                            log.info(
                                    "SERVICE - active supplier not found..."
                            );

                            return new ResourceNotFoundException(
                                    "Active supplier not found"
                            );
                        });


        // -----------------------------------------------------
        // CHECK SUPPLIER INVOICE NUMBER
        // -----------------------------------------------------

        /*
         * Supplier invoice number is optional.
         *
         * Duplicate checking is performed only when
         * an invoice number is actually provided.
         */

        String supplierInvoiceNumber =
                dto.getSupplierInvoiceNumber();

        if (supplierInvoiceNumber != null) {

            supplierInvoiceNumber =
                    supplierInvoiceNumber.trim();

            if (!supplierInvoiceNumber.isBlank()
                    && purchaseRepo
                    .existsBySupplierInvoiceNumberAndSupplier(
                            supplierInvoiceNumber,
                            supplier
                    )) {

                log.info(
                        "SERVICE - supplier invoice number already exists..."
                );

                throw new InvalidRequestException(
                        "Purchase with this supplier invoice number already exists"
                );
            }
        }


        // -----------------------------------------------------
        // CREATE PURCHASE
        // -----------------------------------------------------

        User currentUser = currentUserService.getCurrentUser();

        Purchase purchase =
                new Purchase();

        purchase.setUser(currentUser);

        purchase.setSupplier(
                supplier
        );


        purchase.setRawMaterial(
                dto.getRawMaterial().trim()
        );


        purchase.setWeight(
                dto.getWeight()
        );


        purchase.setUnit(
                dto.getUnit()
        );


        purchase.setRatePerUnit(
                dto.getRatePerUnit()
        );


        purchase.setGstPercentage(
                dto.getGstPercentage()
        );


        purchase.setSupplierInvoiceNumber(
                supplierInvoiceNumber
        );


        purchase.setPurchaseDate(
                dto.getPurchaseDate()
        );


        // -----------------------------------------------------
        // INITIAL STATUS
        // -----------------------------------------------------

        /*
         * Status is controlled by the service.
         *
         * Frontend cannot decide whether a new purchase
         * should be ACTIVE/CANCELLED or PAID/PENDING.
         */

        purchase.setPurchaseStatus(
                PurchaseStatus.ACTIVE
        );


        purchase.setPaymentStatus(
                PaymentStatus.PENDING
        );


        // -----------------------------------------------------
        // CALCULATE AMOUNT
        // -----------------------------------------------------

        BigDecimal amount =
                dto.getWeight()
                        .multiply(
                                dto.getRatePerUnit()
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        purchase.setAmount(
                amount
        );


        // -----------------------------------------------------
        // CALCULATE GST
        // -----------------------------------------------------

        BigDecimal gstAmount =
                amount
                        .multiply(
                                dto.getGstPercentage()
                        )
                        .divide(
                                BigDecimal.valueOf(100),
                                2,
                                RoundingMode.HALF_UP
                        );

        purchase.setGstAmount(
                gstAmount
        );


        // -----------------------------------------------------
        // CALCULATE TOTAL
        // -----------------------------------------------------

        BigDecimal totalAmount =
                amount
                        .add(gstAmount)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        purchase.setTotalAmount(
                totalAmount
        );


        // -----------------------------------------------------
        // GENERATE PURCHASE NUMBER
        // -----------------------------------------------------

        log.info(
                "SERVICE - generating purchase number..."
        );

        int year =
                dto.getPurchaseDate()
                        .getYear();

        String purchaseNumber =
                documentSequenceService
                        .generateDocumentNumber(
                                DocumentType.PURCHASE,
                                year
                        );

        purchase.setPurchaseNumber(
                purchaseNumber
        );


        // -----------------------------------------------------
        // SAVE PURCHASE
        // -----------------------------------------------------

        log.info(
                "SERVICE - saving purchase..."
        );

        purchase =
                purchaseRepo.save(
                        purchase
                );


        // -----------------------------------------------------
        // ADD STOCK
        // -----------------------------------------------------

        /*
         * Every successful purchase automatically creates
         * a PURCHASE_IN stock transaction.
         *
         * Purchase number is used as the reference number.
         */

        log.info(
                "SERVICE - adding purchased quantity to stock..."
        );

        stockTransactionService.purchaseStockIn(
                purchase.getRawMaterial(),
                purchase.getUnit(),
                purchase.getWeight(),
                purchase.getPurchaseNumber()
        );


        log.info(
                "SERVICE - purchase and stock added successfully..."
        );


        return mapToResponse(
                purchase
        );
    }


    // =========================================================
    // GET PURCHASE BY PUBLIC ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ResponsePurchaseDTO getPurchaseByPublicId(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in getPurchaseByPublicId..."
        );


        User currentUser = currentUserService.getCurrentUser();

        Purchase purchase;

        if (currentUser.getRole() == UserRole.ADMIN) {
            purchase = purchaseRepo.findByPublicId(publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - purchase not found...");
                        return new ResourceNotFoundException("Purchase not found");
                    });
        } else {
            purchase = purchaseRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - purchase not found for current user...");
                        return new ResourceNotFoundException("Purchase not found");
                    });
        }

        return mapToResponse(purchase);
    }


    // =========================================================
    // GET ALL PURCHASES
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ResponsePurchaseDTO> getAllPurchases(org.springframework.data.domain.Pageable pageable) {
        return getAllPurchases(null, null, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ResponsePurchaseDTO> getAllPurchases(
            UUID supplierPublicId,
            PaymentStatus paymentStatus,
            LocalDate fromDate,
            LocalDate toDate,
            org.springframework.data.domain.Pageable pageable
    ) {
        log.info("SERVICE - request came in getAllPurchases with filters...");
        User currentUser = currentUserService.getCurrentUser();
        User filterUser = (currentUser.getRole() == UserRole.ADMIN) ? null : currentUser;

        Specification<Purchase> spec = PurchaseSpecification.filter(
                filterUser,
                supplierPublicId,
                paymentStatus,
                fromDate,
                toDate
        );

        return purchaseRepo.findAll(spec, pageable).map(this::mapToResponse);
    }


    // =========================================================
    // GET PURCHASES BY SUPPLIER
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponsePurchaseDTO> getPurchasesBySupplier(
            UUID supplierPublicId
    ) {

        log.info(
                "SERVICE - request came in getPurchasesBySupplier..."
        );


        User currentUser = currentUserService.getCurrentUser();

        Supplier supplier;

        if (currentUser.getRole() == UserRole.ADMIN) {
            supplier = supplierRepo.findByPublicId(supplierPublicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - supplier not found...");
                        return new ResourceNotFoundException("Supplier not found");
                    });
        } else {
            supplier = supplierRepo.findByUserAndPublicId(currentUser, supplierPublicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - supplier not found for current user...");
                        return new ResourceNotFoundException("Supplier not found");
                    });
        }

        return purchaseRepo.findByUser(currentUser).stream()
                .filter(p -> p.getSupplier() != null && p.getSupplier().getPublicId().equals(supplier.getPublicId()))
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET PURCHASES BY DATE RANGE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponsePurchaseDTO> getPurchasesByDateRange(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        log.info(
                "SERVICE - request came in getPurchasesByDateRange..."
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

        return purchaseRepo
                .findByUserAndPurchaseDateBetween(
                        currentUser,
                        fromDate,
                        toDate
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET PURCHASES BY STATUS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponsePurchaseDTO> getPurchasesByStatus(
            String status
    ) {

        log.info(
                "SERVICE - request came in getPurchasesByStatus..."
        );


        if (status == null ||
                status.isBlank()) {

            throw new InvalidRequestException(
                    "Purchase status is required"
            );
        }


        PurchaseStatus purchaseStatus;

        try {

            purchaseStatus =
                    PurchaseStatus.valueOf(
                            status.trim().toUpperCase()
                    );

        } catch (IllegalArgumentException exception) {

            throw new InvalidRequestException(
                    "Invalid purchase status"
            );
        }


        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == UserRole.ADMIN) {
            return purchaseRepo.findByPurchaseStatus(purchaseStatus).stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        return purchaseRepo
                .findByUserAndPurchaseStatus(
                        currentUser,
                        purchaseStatus
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // UPDATE PURCHASE
    // =========================================================

    @Override
    @Transactional
    public ResponsePurchaseDTO updatePurchase(
            UUID publicId,
            RequestPurchaseDTO dto
    ) {
        log.info("SERVICE - request came in updatePurchase: {}", publicId);

        User currentUser = currentUserService.getCurrentUser();
        Purchase purchase;

        if (currentUser.getRole() == UserRole.ADMIN) {
            purchase = purchaseRepo.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
        } else {
            purchase = purchaseRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
        }

        // -----------------------------------------------------
        // CHECK PAYMENT RESTRICTIONS ON SUPPLIER & TOTAL AMOUNT
        // -----------------------------------------------------
        BigDecimal paidAmount = purchasePaymentRepo.sumPaidAmountByPurchase(purchase);
        boolean hasPayments = (paidAmount != null && paidAmount.compareTo(BigDecimal.ZERO) > 0)
                || purchasePaymentRepo.existsByPurchase(purchase);

        Supplier supplier;
        if (currentUser.getRole() == UserRole.ADMIN) {
            supplier = supplierRepo.findByPublicIdAndIsActiveTrue(dto.getSupplierPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Active supplier not found"));
        } else {
            supplier = supplierRepo.findByUserAndPublicIdAndIsActiveTrue(currentUser, dto.getSupplierPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Active supplier not found"));
        }

        // Rule: Only Supplier field should not be editable if payments have been recorded
        if (hasPayments && !purchase.getSupplier().getPublicId().equals(supplier.getPublicId())) {
            throw new InvalidRequestException(
                    "Supplier cannot be changed because payments have already been recorded against this purchase."
            );
        }

        // -----------------------------------------------------
        // SUPPLIER INVOICE NUMBER VALIDATION
        // -----------------------------------------------------
        String newInvoiceNumber = dto.getSupplierInvoiceNumber();
        if (newInvoiceNumber != null) {
            newInvoiceNumber = newInvoiceNumber.trim();
            if (newInvoiceNumber.isBlank()) {
                newInvoiceNumber = null;
            }
        }

        String oldInvoiceNumber = purchase.getSupplierInvoiceNumber();
        boolean invoiceChanged = !Objects.equals(oldInvoiceNumber, newInvoiceNumber);
        boolean supplierChanged = !purchase.getSupplier().getPublicId().equals(supplier.getPublicId());

        if ((invoiceChanged || supplierChanged)
                && newInvoiceNumber != null
                && purchaseRepo.existsBySupplierInvoiceNumberAndSupplier(newInvoiceNumber, supplier)) {
            throw new InvalidRequestException(
                    "Purchase with this supplier invoice number already exists"
            );
        }

        // -----------------------------------------------------
        // SYNCHRONIZE STOCK LEDGER IF MATERIAL / UNIT / WEIGHT CHANGED
        // -----------------------------------------------------
        String oldRawMaterial = purchase.getRawMaterial();
        WeightUnit oldUnit = purchase.getUnit();
        BigDecimal oldWeight = purchase.getWeight();

        String newRawMaterial = dto.getRawMaterial().trim();
        WeightUnit newUnit = dto.getUnit();
        BigDecimal newWeight = dto.getWeight();

        boolean materialChanged = !oldRawMaterial.equalsIgnoreCase(newRawMaterial) || oldUnit != newUnit;
        if (materialChanged && hasPayments) {
            throw new InvalidRequestException(
                    "Raw material cannot be changed because payments have already been recorded against this purchase bill."
            );
        }

        boolean stockChanged = materialChanged || oldWeight.compareTo(newWeight) != 0;

        if (stockChanged) {
            stockTransactionService.updatePurchaseStock(
                    currentUser,
                    oldRawMaterial,
                    oldUnit,
                    oldWeight,
                    newRawMaterial,
                    newUnit,
                    newWeight,
                    purchase.getPurchaseNumber()
            );
        }

        // -----------------------------------------------------
        // RECALCULATE AMOUNT, GST & TOTAL
        // -----------------------------------------------------
        BigDecimal amount = newWeight.multiply(dto.getRatePerUnit()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gstAmount = amount.multiply(dto.getGstPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = amount.add(gstAmount).setScale(2, RoundingMode.HALF_UP);

        if (paidAmount != null && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (totalAmount.compareTo(paidAmount) < 0) {
                throw new InvalidRequestException(
                        "Updated total amount (₹" + totalAmount + ") cannot be less than the already paid amount (₹" + paidAmount + ")."
                );
            }
        }

        // -----------------------------------------------------
        // UPDATE PURCHASE FIELDS
        // -----------------------------------------------------
        purchase.setSupplier(supplier);
        purchase.setRawMaterial(newRawMaterial);
        purchase.setWeight(newWeight);
        purchase.setUnit(newUnit);
        purchase.setRatePerUnit(dto.getRatePerUnit());
        purchase.setGstPercentage(dto.getGstPercentage());
        purchase.setSupplierInvoiceNumber(newInvoiceNumber);
        purchase.setPurchaseDate(dto.getPurchaseDate());
        purchase.setAmount(amount);
        purchase.setGstAmount(gstAmount);
        purchase.setTotalAmount(totalAmount);

        // Update payment status
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            purchase.setPaymentStatus(PaymentStatus.PENDING);
        } else if (paidAmount.compareTo(totalAmount) >= 0) {
            purchase.setPaymentStatus(PaymentStatus.COMPLETED);
        } else {
            purchase.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        purchase = purchaseRepo.save(purchase);
        log.info("SERVICE - purchase updated successfully: {}", purchase.getPurchaseNumber());
        return mapToResponse(purchase);
    }


    // =========================================================
    // DELETE PURCHASE
    // =========================================================

    @Override
    @Transactional
    public void deletePurchase(UUID publicId) {
        log.info("SERVICE - request came in deletePurchase: {}", publicId);

        User currentUser = currentUserService.getCurrentUser();
        Purchase purchase;

        if (currentUser.getRole() == UserRole.ADMIN) {
            purchase = purchaseRepo.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
        } else {
            purchase = purchaseRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
        }

        // -----------------------------------------------------
        // VERIFY ZERO PAYMENTS
        // -----------------------------------------------------
        BigDecimal paidAmount = purchasePaymentRepo.sumPaidAmountByPurchase(purchase);
        boolean hasPayments = (paidAmount != null && paidAmount.compareTo(BigDecimal.ZERO) > 0)
                || purchasePaymentRepo.existsByPurchase(purchase);

        if (hasPayments) {
            throw new InvalidRequestException(
                    "Cannot delete purchase #" + purchase.getPurchaseNumber() +
                            ": Payments have already been recorded against this purchase. " +
                            "Please delete or reverse all associated payments in the Purchase Payments tab before deleting this purchase."
            );
        }

        // -----------------------------------------------------
        // REVERT STOCK
        // -----------------------------------------------------
        stockTransactionService.revertPurchaseStock(
                currentUser,
                purchase.getRawMaterial(),
                purchase.getUnit(),
                purchase.getWeight(),
                purchase.getPurchaseNumber()
        );

        // -----------------------------------------------------
        // DELETE ENTITY
        // -----------------------------------------------------
        purchaseRepo.delete(purchase);
        log.info("SERVICE - purchase {} deleted successfully", purchase.getPurchaseNumber());
    }



    // =========================================================
    // ENTITY → RESPONSE DTO
    // =========================================================

    private ResponsePurchaseDTO mapToResponse(
            Purchase purchase
    ) {

        log.info(
                "SERVICE - mapping purchase to response DTO..."
        );


        // -----------------------------------------------------
        // MAP PURCHASE FIELDS
        // -----------------------------------------------------

        ResponsePurchaseDTO response =
                modelMapper.map(
                        purchase,
                        ResponsePurchaseDTO.class
                );


        // -----------------------------------------------------
        // MAP SUPPLIER DETAILS
        // -----------------------------------------------------

        Supplier supplier =
                purchase.getSupplier();


        if (supplier != null) {

            ResponsePurchaseDTO.SupplierDetails
                    supplierDetails =
                    new ResponsePurchaseDTO.SupplierDetails();


            supplierDetails.setPublicId(
                    supplier.getPublicId()
            );


            supplierDetails.setSupplierName(
                    supplier.getSupplierName()
            );


            supplierDetails.setMobileNumber(
                    supplier.getMobileNumber()
            );


            supplierDetails.setEmail(
                    supplier.getEmail()
            );


            supplierDetails.setGstNumber(
                    supplier.getGstNumber()
            );


            response.setSupplier(
                    supplierDetails
            );
        }


        return response;
    }
}
