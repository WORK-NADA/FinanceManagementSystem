package FinanceManangementSystem.demo.Repository;

import FinanceManangementSystem.demo.Model.Partner;
import FinanceManangementSystem.demo.Model.PartnerProfitWithdrawal;
import FinanceManangementSystem.demo.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PartnerProfitWithdrawalRepository
        extends JpaRepository<PartnerProfitWithdrawal, Long> {

    List<PartnerProfitWithdrawal> findByUserOrderByWithdrawalDateDescCreatedAtDesc(User user);

    List<PartnerProfitWithdrawal> findByUserAndWithdrawalDateBetweenOrderByWithdrawalDateDescCreatedAtDesc(
            User user,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<PartnerProfitWithdrawal> findByUserAndPartnerOrderByWithdrawalDateDescCreatedAtDesc(
            User user,
            Partner partner
    );

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM PartnerProfitWithdrawal w WHERE w.partner = :partner AND w.user = :user")
    BigDecimal sumWithdrawnByPartnerAndUser(
            @Param("partner") Partner partner,
            @Param("user") User user
    );

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM PartnerProfitWithdrawal w WHERE w.user = :user")
    BigDecimal sumWithdrawnByUser(@Param("user") User user);
}
