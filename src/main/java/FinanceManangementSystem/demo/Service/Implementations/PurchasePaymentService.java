package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Exceptions.InvalidRequestException;
import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;

import FinanceManangementSystem.demo.Enums.DocumentType;
import FinanceManangementSystem.demo.Enums.PaymentStatus;
import FinanceManangementSystem.demo.Enums.UserRole;
import FinanceManangementSystem.demo.Model.Purchase;
import FinanceManangementSystem.demo.Model.PurchasePayment;
import FinanceManangementSystem.demo.Model.Supplier;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestPurchasePaymentDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponsePurchasePaymentDTO;
import FinanceManangementSystem.demo.Repository.PurchasePaymentRepository;
import FinanceManangementSystem.demo.Repository.PurchaseRepository;
import FinanceManangementSystem.demo.Repository.SupplierRepository;
import FinanceManangementSystem.demo.Service.PurchasePaymentServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchasePaymentService
        implements PurchasePaymentServiceInterface {

    private final PurchasePaymentRepository purchasePaymentRepo;

    private final PurchaseRepository purchaseRepo;

    private final CurrentUserService currentUserService;

    private final SupplierRepository supplierRepo;

    private final DocumentSequenceService documentSequenceService;

    private final ModelMapper modelMapper;


    // =========================================================
    // ADD PAYMENT
    // =========================================================

    @Override
    @Transactional
    public ResponsePurchasePaymentDTO addPayment(
            RequestPurchasePaymentDTO dto
    ) {

        log.info(
                "SERVICE - request came in addPayment..."
        );


        // -----------------------------------------------------
        // FIND PURCHASE
        // -----------------------------------------------------

        User currentUser = currentUserService.getCurrentUser();

        Purchase purchase =
                purchaseRepo
                        .findByUserAndPublicId(
                                currentUser,
                                dto.getPurchasePublicId()
                        )
                        .orElseThrow(() -> {

                            log.info(
                                    "SERVICE - purchase not found..."
                            );

                            return new ResourceNotFoundException(
                                    "Purchase not found"
                            );
                        });


        // -----------------------------------------------------
        // VALIDATE AMOUNT
        // -----------------------------------------------------

        BigDecimal currentPaidAmount =
                purchasePaymentRepo
                        .sumPaidAmountByPurchase(
                                purchase
                        );

        BigDecimal newTotalPaid =
                currentPaidAmount
                        .add(
                                dto.getAmountPaid()
                        );

        if (newTotalPaid.compareTo(purchase.getTotalAmount()) > 0) {

            log.info(
                    "SERVICE - payment amount exceeds pending balance..."
            );

            BigDecimal currentPending =
                    purchase
                            .getTotalAmount()
                            .subtract(
                                    currentPaidAmount
                            )
                            .max(
                                    BigDecimal.ZERO
                            );

            throw new InvalidRequestException(
                    "Payment amount exceeds remaining pending amount of " + currentPending
            );
        }


        // -----------------------------------------------------
        // GENERATE PAYMENT NUMBER (system-assigned, sequential)
        // -----------------------------------------------------

        int year =
                dto.getPaymentDate()
                        .getYear();

        String paymentNumber =
                documentSequenceService
                        .generateDocumentNumber(
                                DocumentType.PURCHASE_PAYMENT,
                                year
                        );


        // -----------------------------------------------------
        // CREATE PAYMENT ENTITY
        // -----------------------------------------------------

        PurchasePayment payment =
                new PurchasePayment();

        payment.setUser(currentUser);

        payment.setPurchase(
                purchase
        );

        payment.setAmountPaid(
                dto.getAmountPaid()
        );

        payment.setPaymentDate(
                dto.getPaymentDate()
        );

        payment.setPaymentMode(
                dto.getPaymentMode()
        );

        payment.setPaymentNumber(
                paymentNumber
        );

        payment.setReferenceNumber(
                dto.getReferenceNumber() != null ? dto.getReferenceNumber().trim() : null
        );

        payment.setRemarks(
                dto.getRemarks() != null ? dto.getRemarks().trim() : null
        );

        payment =
                purchasePaymentRepo.save(
                        payment
                );


        // -----------------------------------------------------
        // UPDATE PURCHASE PAYMENT STATUS
        // -----------------------------------------------------

        if (newTotalPaid.compareTo(purchase.getTotalAmount()) >= 0) {

            purchase.setPaymentStatus(
                    PaymentStatus.COMPLETED
            );

        } else if (newTotalPaid.compareTo(BigDecimal.ZERO) > 0) {

            purchase.setPaymentStatus(
                    PaymentStatus.PARTIALLY_PAID
            );

        } else {

            purchase.setPaymentStatus(
                    PaymentStatus.PENDING
            );
        }

        purchaseRepo.save(
                purchase
        );


        log.info(
                "SERVICE - purchase payment added successfully..."
        );


        return mapToResponse(
                payment,
                purchase,
                newTotalPaid
        );
    }


    // =========================================================
    // GET PAYMENT BY PUBLIC ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ResponsePurchasePaymentDTO getPaymentByPublicId(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in getPaymentByPublicId..."
        );

        User currentUser = currentUserService.getCurrentUser();

        PurchasePayment payment;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            payment = purchasePaymentRepo.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase payment not found"));
        } else {
            payment = purchasePaymentRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase payment not found"));
        }

        Purchase purchase =
                payment.getPurchase();

        BigDecimal paidAmount =
                purchasePaymentRepo
                        .sumPaidAmountByPurchase(
                                purchase
                        );

        return mapToResponse(
                payment,
                purchase,
                paidAmount
        );
    }


    // =========================================================
    // GET PAYMENTS BY PURCHASE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponsePurchasePaymentDTO> getPaymentsByPurchase(
            UUID purchasePublicId
    ) {

        log.info(
                "SERVICE - request came in getPaymentsByPurchase..."
        );

        User currentUser = currentUserService.getCurrentUser();

        Purchase purchase;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            purchase = purchaseRepo.findByPublicId(purchasePublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
        } else {
            purchase = purchaseRepo.findByUserAndPublicId(currentUser, purchasePublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
        }

        BigDecimal paidAmount =
                purchasePaymentRepo
                        .sumPaidAmountByPurchase(
                                purchase
                        );

        return purchasePaymentRepo
                .findByPurchaseOrderByPaymentDateDesc(
                        purchase
                )
                .stream()
                .map(payment -> mapToResponse(payment, purchase, paidAmount))
                .toList();
    }


    // =========================================================
    // GET PAYMENTS BY SUPPLIER
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponsePurchasePaymentDTO> getPaymentsBySupplier(
            UUID supplierPublicId
    ) {

        log.info(
                "SERVICE - request came in getPaymentsBySupplier..."
        );

        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            supplierRepo.findByPublicIdAndIsActiveTrue(supplierPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Active supplier not found"));
            return purchasePaymentRepo
                    .findByPurchase_Supplier_PublicIdOrderByPaymentDateDesc(supplierPublicId)
                    .stream()
                    .map(payment -> {
                        Purchase purchase = payment.getPurchase();
                        BigDecimal paidAmount = purchasePaymentRepo.sumPaidAmountByPurchase(purchase);
                        return mapToResponse(payment, purchase, paidAmount);
                    })
                    .toList();
        }

        supplierRepo.findByUserAndPublicIdAndIsActiveTrue(currentUser, supplierPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Active supplier not found"));

        return purchasePaymentRepo
                .findByUserAndPurchase_Supplier_PublicIdOrderByPaymentDateDesc(
                        currentUser,
                        supplierPublicId
                )
                .stream()
                .map(payment -> {
                    Purchase purchase = payment.getPurchase();
                    BigDecimal paidAmount = purchasePaymentRepo.sumPaidAmountByPurchase(purchase);
                    return mapToResponse(payment, purchase, paidAmount);
                })
                .toList();
    }


    // =========================================================
    // GET PURCHASE PAYMENT SUMMARY
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ResponsePurchasePaymentDTO.PurchaseDetails getPurchasePaymentSummary(
            UUID purchasePublicId
    ) {

        log.info(
                "SERVICE - request came in getPurchasePaymentSummary..."
        );

        User currentUser = currentUserService.getCurrentUser();

        Purchase purchase;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            purchase = purchaseRepo.findByPublicId(purchasePublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
        } else {
            purchase = purchaseRepo.findByUserAndPublicId(currentUser, purchasePublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
        }

        return buildPurchaseDetails(
                purchase
        );
    }


    // =========================================================
    // GET ALL PENDING PAYMENTS (DASHBOARD)
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponsePurchasePaymentDTO.PurchaseDetails> getAllPendingPayments() {

        log.info(
                "SERVICE - request came in getAllPendingPayments..."
        );

        User currentUser = currentUserService.getCurrentUser();

        List<Purchase> pendingPurchases;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            pendingPurchases = purchaseRepo.findByPaymentStatusIn(List.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.PARTIALLY_PAID
            ));
        } else {
            pendingPurchases = purchaseRepo.findByUserAndPaymentStatusIn(currentUser, List.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.PARTIALLY_PAID
            ));
        }

        return pendingPurchases
                .stream()
                .map(this::buildPurchaseDetails)
                .toList();
    }

        @Override
        @Transactional(readOnly = true)
        public Page<ResponsePurchasePaymentDTO> getAllPayments(@org.springframework.lang.NonNull org.springframework.data.domain.Pageable pageable) {

                log.info("SERVICE - request came in getAllPayments...");

                User currentUser = currentUserService.getCurrentUser();

                if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
                        return purchasePaymentRepo.findAll(pageable)
                                        .map(payment -> {
                                                Purchase purchase = payment.getPurchase();
                                                java.math.BigDecimal paidAmount = purchasePaymentRepo.sumPaidAmountByPurchase(purchase);
                                                return mapToResponse(payment, purchase, paidAmount);
                                        });
                }

                List<ResponsePurchasePaymentDTO> filtered = purchasePaymentRepo.findAll(pageable)
                                .getContent()
                                .stream()
                                .filter(payment -> payment.getUser() != null && payment.getUser().equals(currentUser))
                                .map(payment -> {
                                        Purchase purchase = payment.getPurchase();
                                        java.math.BigDecimal paidAmount = purchasePaymentRepo.sumPaidAmountByPurchase(purchase);
                                        return mapToResponse(payment, purchase, paidAmount);
                                })
                                .toList();

                return new PageImpl<>(filtered, pageable, filtered.size());
        }


    // =========================================================
    // GET PENDING PAYMENTS BY SUPPLIER (DASHBOARD)
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponsePurchasePaymentDTO.PurchaseDetails> getPendingPaymentsBySupplier(
            UUID supplierPublicId
    ) {

        log.info(
                "SERVICE - request came in getPendingPaymentsBySupplier..."
        );

        User currentUser = currentUserService.getCurrentUser();

        Supplier supplier;

        if (currentUser.getRole() == UserRole.ADMIN) {
            supplier = supplierRepo.findByPublicIdAndIsActiveTrue(supplierPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Active supplier not found"));
            List<Purchase> pendingPurchases = purchaseRepo.findBySupplierAndPaymentStatusIn(supplier, List.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.PARTIALLY_PAID
            ));
            return pendingPurchases.stream().map(this::buildPurchaseDetails).toList();
        }

        supplier = supplierRepo.findByUserAndPublicIdAndIsActiveTrue(currentUser, supplierPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Active supplier not found"));

        List<Purchase> pendingPurchases = purchaseRepo.findByUser(currentUser).stream()
                .filter(p -> p.getSupplier() != null && p.getSupplier().getPublicId().equals(supplier.getPublicId()))
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PENDING || p.getPaymentStatus() == PaymentStatus.PARTIALLY_PAID)
                .toList();

        return pendingPurchases
                .stream()
                .map(this::buildPurchaseDetails)
                .toList();
    }


    // =========================================================
    // GET TOTAL OUTSTANDING AMOUNT (DASHBOARD SUMMARY)
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalOutstandingAmount() {

        log.info(
                "SERVICE - request came in getTotalOutstandingAmount..."
        );

        User currentUser = currentUserService.getCurrentUser();

        List<Purchase> pendingPurchases;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            pendingPurchases = purchaseRepo.findByPaymentStatusIn(List.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.PARTIALLY_PAID
            ));
        } else {
            pendingPurchases = purchaseRepo.findByUserAndPaymentStatusIn(currentUser, List.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.PARTIALLY_PAID
            ));
        }

        return pendingPurchases
                .stream()
                .map(purchase -> {
                    BigDecimal paid = purchasePaymentRepo.sumPaidAmountByPurchase(purchase);
                    return purchase.getTotalAmount().subtract(paid).max(BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    // =========================================================
    // HELPER: BUILD PURCHASE DETAILS
    // =========================================================

    private ResponsePurchasePaymentDTO.PurchaseDetails buildPurchaseDetails(
            Purchase purchase
    ) {

        BigDecimal paidAmount =
                purchasePaymentRepo
                        .sumPaidAmountByPurchase(
                                purchase
                        );

        BigDecimal pendingAmount =
                purchase
                        .getTotalAmount()
                        .subtract(
                                paidAmount
                        )
                        .max(
                                BigDecimal.ZERO
                        );

        ResponsePurchasePaymentDTO.PurchaseDetails details =
                new ResponsePurchasePaymentDTO.PurchaseDetails();

        details.setPublicId(
                purchase.getPublicId()
        );

        details.setPurchaseNumber(
                purchase.getPurchaseNumber()
        );

        details.setSupplierName(
                purchase.getSupplier() != null
                        ? purchase.getSupplier().getSupplierName()
                        : null
        );

        details.setPurchaseDate(
                purchase.getPurchaseDate()
        );

        details.setTotalAmount(
                purchase.getTotalAmount()
        );

        details.setPaidAmount(
                paidAmount
        );

        details.setPendingAmount(
                pendingAmount
        );

        details.setPaymentStatus(
                purchase.getPaymentStatus()
        );

        return details;
    }


    // =========================================================
    // HELPER: MAP TO RESPONSE DTO
    // =========================================================

    private ResponsePurchasePaymentDTO mapToResponse(
            PurchasePayment payment,
            Purchase purchase,
            BigDecimal paidAmount
    ) {

        ResponsePurchasePaymentDTO response =
                modelMapper.map(
                        payment,
                        ResponsePurchasePaymentDTO.class
                );

        response.setPurchase(
                buildPurchaseDetails(
                        purchase
                )
        );

        return response;
    }
}
