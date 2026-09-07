package FinanceManangementSystem.demo.Repository;

import FinanceManangementSystem.demo.Model.Purchase;
import FinanceManangementSystem.demo.Model.PurchasePayment;
import FinanceManangementSystem.demo.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchasePaymentRepository
        extends JpaRepository<PurchasePayment, Long> {

    Optional<PurchasePayment> findByPublicId(
            UUID publicId
    );

    Optional<PurchasePayment> findByUserAndPublicId(User user, UUID publicId);

    List<PurchasePayment> findByUserAndPurchaseOrderByPaymentDateDesc(User user, Purchase purchase);

    List<PurchasePayment> findByUserAndPurchase_Supplier_PublicIdOrderByPaymentDateDesc(User user, UUID supplierPublicId);

    List<PurchasePayment> findByPurchaseOrderByPaymentDateDesc(
            Purchase purchase
    );

    List<PurchasePayment> findByPurchase_Supplier_PublicIdOrderByPaymentDateDesc(
            UUID supplierPublicId
    );

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM PurchasePayment p WHERE p.purchase = :purchase")
    BigDecimal sumPaidAmountByPurchase(
            @Param("purchase") Purchase purchase
    );

    boolean existsByReferenceNumber(
            String referenceNumber
    );

    boolean existsByPurchase(
            Purchase purchase
    );

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM PurchasePayment p WHERE p.user = :user")
    BigDecimal sumTotalPaidByUser(
            @Param("user") User user
    );

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM PurchasePayment p")
    BigDecimal sumTotalPaid();

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM PurchasePayment p WHERE p.user = :user AND p.paymentDate BETWEEN :fromDate AND :toDate")
    BigDecimal sumTotalPaidByUserAndDateRange(
            @Param("user") User user,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
