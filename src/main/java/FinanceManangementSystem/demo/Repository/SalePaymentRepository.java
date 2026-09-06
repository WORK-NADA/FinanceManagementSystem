package FinanceManangementSystem.demo.Repository;

import FinanceManangementSystem.demo.Model.Sale;
import FinanceManangementSystem.demo.Model.SalePayment;
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
public interface SalePaymentRepository
        extends JpaRepository<SalePayment, Long> {

    Optional<SalePayment> findByPublicId(
            UUID publicId
    );

    Optional<SalePayment> findByUserAndPublicId(User user, UUID publicId);

    List<SalePayment> findByUserAndSaleOrderByPaymentDateDesc(User user, Sale sale);

    List<SalePayment> findByUserAndSale_Customer_PublicIdOrderByPaymentDateDesc(User user, UUID customerPublicId);

    List<SalePayment> findBySaleOrderByPaymentDateDesc(
            Sale sale
    );

    List<SalePayment> findBySale_Customer_PublicIdOrderByPaymentDateDesc(
            UUID customerPublicId
    );

    @Query("SELECT COALESCE(SUM(p.amountReceived), 0) FROM SalePayment p WHERE p.sale = :sale")
    BigDecimal sumReceivedAmountBySale(
            @Param("sale") Sale sale
    );

    boolean existsByReferenceNumber(
            String referenceNumber
    );

    boolean existsBySale(
            Sale sale
    );

    @Query("SELECT COALESCE(SUM(p.amountReceived), 0) FROM SalePayment p WHERE p.user = :user")
    BigDecimal sumTotalReceivedByUser(
            @Param("user") User user
    );

    @Query("SELECT COALESCE(SUM(p.amountReceived), 0) FROM SalePayment p WHERE p.user = :user AND p.paymentDate BETWEEN :fromDate AND :toDate")
    BigDecimal sumTotalReceivedByUserAndDateRange(
            @Param("user") User user,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
