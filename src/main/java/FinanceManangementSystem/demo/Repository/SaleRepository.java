package FinanceManangementSystem.demo.Repository;

import FinanceManangementSystem.demo.Enums.PaymentStatus;
import FinanceManangementSystem.demo.Model.Customer;
import FinanceManangementSystem.demo.Model.Sale;
import FinanceManangementSystem.demo.Model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;

@Repository
public interface SaleRepository
        extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    // =========================================================
    // FIND BY PUBLIC ID
    // =========================================================

    Optional<Sale> findByPublicId(
            UUID publicId
    );

    Optional<Sale> findByUserAndPublicId(User user, UUID publicId);

    List<Sale> findByUser(User user);

    Page<Sale> findByUser(User user, Pageable pageable);

    List<Sale> findByUserAndSaleDateBetween(User user, LocalDate fromDate, LocalDate toDate);

    List<Sale> findByUserAndPaymentStatusIn(User user, List<PaymentStatus> statuses);

    // =========================================================
    // FIND BY SALE NUMBER
    // =========================================================

    Optional<Sale> findBySaleNumber(
            String saleNumber
    );


    // =========================================================
    // CHECK SALE NUMBER
    // =========================================================

    boolean existsBySaleNumber(
            String saleNumber
    );


    // =========================================================
    // FIND BY CUSTOMER
    // =========================================================

    List<Sale> findByCustomer(
            Customer customer
    );


    // =========================================================
    // CHECK CUSTOMER INVOICE NUMBER
    // =========================================================

    boolean existsByCustomerInvoiceNumberAndCustomer(
            String customerInvoiceNumber,
            Customer customer
    );


    // =========================================================
    // FIND BY SALE DATE RANGE
    // =========================================================

    List<Sale> findBySaleDateBetween(
            LocalDate fromDate,
            LocalDate toDate
    );


    // =========================================================
    // FIND BY PAYMENT STATUSES (DASHBOARD)
    // =========================================================

    List<Sale> findByPaymentStatusIn(
            List<PaymentStatus> statuses
    );


    // =========================================================
    // FIND BY CUSTOMER AND PAYMENT STATUSES (DASHBOARD)
    // =========================================================

    List<Sale> findByCustomerAndPaymentStatusIn(
            Customer customer,
            List<PaymentStatus> statuses
    );

    // =========================================================
    // FIND BY RAW MATERIAL (FOR SYNCHRONIZATION)
    // =========================================================

    List<Sale> findByUserAndRawMaterialIgnoreCase(
            User user,
            String rawMaterial
    );

    List<Sale> findByRawMaterialIgnoreCase(
            String rawMaterial
    );
}
