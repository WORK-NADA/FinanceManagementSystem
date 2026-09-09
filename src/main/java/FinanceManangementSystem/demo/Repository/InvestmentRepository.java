package FinanceManangementSystem.demo.Repository;

import FinanceManangementSystem.demo.Model.Investment;
import FinanceManangementSystem.demo.Model.Partner;
import FinanceManangementSystem.demo.Model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    Optional<Investment> findByUserAndPublicId(User user, UUID publicId);

    List<Investment> findByUserOrderByInvestmentDateDescCreatedAtDesc(User user);

    Page<Investment> findByUserOrderByInvestmentDateDescCreatedAtDesc(User user, Pageable pageable);

    List<Investment> findByUserAndPartnerOrderByInvestmentDateDesc(User user, Partner partner);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Investment i WHERE i.user = :user")
    BigDecimal sumTotalInvestmentsByUser(@Param("user") User user);
}
