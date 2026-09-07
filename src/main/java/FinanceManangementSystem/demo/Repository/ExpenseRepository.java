package FinanceManangementSystem.demo.Repository;

import FinanceManangementSystem.demo.Enums.ExpenseCategory;
import FinanceManangementSystem.demo.Model.Expense;
import FinanceManangementSystem.demo.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;

@Repository
public interface ExpenseRepository
        extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    Optional<Expense> findByPublicId(
            UUID publicId
    );

    Optional<Expense> findByUserAndPublicId(User user, UUID publicId);

    Optional<Expense> findByUserAndPublicIdAndIsActiveTrue(User user, UUID publicId);

    List<Expense> findByUserAndIsActiveTrueOrderByExpenseDateDesc(User user);

    org.springframework.data.domain.Page<Expense> findByUserAndIsActiveTrueOrderByExpenseDateDesc(User user, org.springframework.data.domain.Pageable pageable);

    Optional<Expense> findByPublicIdAndIsActiveTrue(
            UUID publicId
    );

        List<Expense> findByIsActiveTrueOrderByExpenseDateDesc();

        org.springframework.data.domain.Page<Expense> findByIsActiveTrueOrderByExpenseDateDesc(org.springframework.data.domain.Pageable pageable);

    List<Expense> findByExpenseDateBetweenAndIsActiveTrueOrderByExpenseDateDesc(
            LocalDate fromDate,
            LocalDate toDate
    );

    List<Expense> findByUserAndExpenseDateBetweenAndIsActiveTrueOrderByExpenseDateDesc(
            User user,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<Expense> findByUserAndCategoryAndIsActiveTrueOrderByExpenseDateDesc(
            User user,
            ExpenseCategory category
    );

    List<Expense> findByCategoryAndIsActiveTrueOrderByExpenseDateDesc(
            ExpenseCategory category
    );

    boolean existsByExpenseNumber(
            String expenseNumber
    );

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.isActive = true AND (:fromDate IS NULL OR e.expenseDate >= :fromDate) AND (:toDate IS NULL OR e.expenseDate <= :toDate)")
    BigDecimal sumTotalExpensesByDateRange(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user AND e.isActive = true AND (:fromDate IS NULL OR e.expenseDate >= :fromDate) AND (:toDate IS NULL OR e.expenseDate <= :toDate)")
    BigDecimal sumTotalExpensesByUserAndDateRange(
            @Param("user") User user,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.isActive = true AND (:fromDate IS NULL OR e.expenseDate >= :fromDate) AND (:toDate IS NULL OR e.expenseDate <= :toDate) GROUP BY e.category")
    List<Object[]> findCategoryWiseBreakdownByDateRange(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user AND e.isActive = true AND (:fromDate IS NULL OR e.expenseDate >= :fromDate) AND (:toDate IS NULL OR e.expenseDate <= :toDate) GROUP BY e.category")
    List<Object[]> findCategoryWiseBreakdownByUserAndDateRange(
            @Param("user") User user,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
