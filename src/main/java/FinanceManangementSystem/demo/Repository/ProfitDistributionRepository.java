package FinanceManangementSystem.demo.Repository;

import FinanceManangementSystem.demo.Model.ProfitDistribution;
import FinanceManangementSystem.demo.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfitDistributionRepository
        extends JpaRepository<ProfitDistribution, Long> {

    // =========================================================
    // FIND BY PUBLIC ID
    // =========================================================

    Optional<ProfitDistribution> findByPublicId(
            UUID publicId
    );

    Optional<ProfitDistribution> findByUserAndPublicId(
            User user,
            UUID publicId
    );


    // =========================================================
    // FIND ALL ORDERED BY DATE & LATEST ACTIVITY
    // =========================================================

    List<ProfitDistribution> findAllByOrderByToDateDesc();

    List<ProfitDistribution> findByUserOrderByToDateDesc(
            User user
    );

    @Query("SELECT p FROM ProfitDistribution p ORDER BY COALESCE(p.updatedAt, p.createdAt) DESC, p.id DESC")
    List<ProfitDistribution> findAllOrderByLatestActivity();

    @Query("SELECT p FROM ProfitDistribution p WHERE p.user = :user ORDER BY COALESCE(p.updatedAt, p.createdAt) DESC, p.id DESC")
    List<ProfitDistribution> findByUserOrderByLatestActivity(
            @Param("user") User user
    );


    // =========================================================
    // CHECK & FIND DUPLICATE PERIOD
    // =========================================================

    boolean existsByFromDateAndToDate(
            LocalDate fromDate,
            LocalDate toDate
    );

    boolean existsByUserAndFromDateAndToDate(
            User user,
            LocalDate fromDate,
            LocalDate toDate
    );

    Optional<ProfitDistribution> findByUserAndFromDateAndToDate(
            User user,
            LocalDate fromDate,
            LocalDate toDate
    );


    // =========================================================
    // LATEST DISTRIBUTION
    // =========================================================

    Optional<ProfitDistribution> findFirstByOrderByToDateDesc();

    Optional<ProfitDistribution> findFirstByUserOrderByToDateDesc(
            User user
    );
}
