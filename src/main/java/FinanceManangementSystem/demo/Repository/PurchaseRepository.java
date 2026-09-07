package FinanceManangementSystem.demo.Repository;

import FinanceManangementSystem.demo.Enums.PaymentStatus;
import FinanceManangementSystem.demo.Enums.PurchaseStatus;
import FinanceManangementSystem.demo.Model.Purchase;
import FinanceManangementSystem.demo.Model.Supplier;
import FinanceManangementSystem.demo.Model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseRepository
        extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {

    // =========================================================
    // FIND BY PUBLIC ID
    // =========================================================

    Optional<Purchase> findByPublicId(
            UUID publicId
    );

    Optional<Purchase> findByUserAndPublicId(User user, UUID publicId);

    List<Purchase> findByUser(User user);

    Page<Purchase> findByUser(User user, Pageable pageable);

    List<Purchase> findByUserAndPurchaseDateBetween(User user, LocalDate fromDate, LocalDate toDate);

    List<Purchase> findByUserAndPurchaseStatus(User user, PurchaseStatus purchaseStatus);

    List<Purchase> findByUserAndPaymentStatusIn(User user, List<PaymentStatus> statuses);

    // =========================================================
    // FIND BY PURCHASE NUMBER
    // =========================================================

    Optional<Purchase> findByPurchaseNumber(
            String purchaseNumber
    );


    // =========================================================
    // CHECK PURCHASE NUMBER
    // =========================================================

    boolean existsByPurchaseNumber(
            String purchaseNumber
    );


    // =========================================================
    // FIND BY SUPPLIER
    // =========================================================

    List<Purchase> findBySupplier(
            Supplier supplier
    );


    // =========================================================
    // FIND BY PURCHASE STATUS
    // =========================================================

    List<Purchase> findByPurchaseStatus(
            PurchaseStatus purchaseStatus
    );


    // =========================================================
    // FIND BY PURCHASE DATE RANGE
    // =========================================================

    List<Purchase> findByPurchaseDateBetween(
            LocalDate fromDate,
            LocalDate toDate
    );


    // =========================================================
    // FIND BY SUPPLIER AND STATUS
    // =========================================================

    List<Purchase> findBySupplierAndPurchaseStatus(
            Supplier supplier,
            PurchaseStatus purchaseStatus
    );


    // =========================================================
    // FIND BY PAYMENT STATUSES (DASHBOARD)
    // =========================================================

    List<Purchase> findByPaymentStatusIn(
            List<PaymentStatus> statuses
    );


    // =========================================================
    // FIND BY SUPPLIER AND PAYMENT STATUSES (DASHBOARD)
    // =========================================================

    List<Purchase> findBySupplierAndPaymentStatusIn(
            Supplier supplier,
            List<PaymentStatus> statuses
    );


    // =========================================================
    // CHECK SUPPLIER INVOICE NUMBER
    // =========================================================

    boolean existsBySupplierInvoiceNumberAndSupplier(
            String supplierInvoiceNumber,
            Supplier supplier
    );

    // =========================================================
    // FIND BY RAW MATERIAL (FOR SYNCHRONIZATION)
    // =========================================================

    List<Purchase> findByUserAndRawMaterialIgnoreCase(
            User user,
            String rawMaterial
    );

    List<Purchase> findByRawMaterialIgnoreCase(
            String rawMaterial
    );
}