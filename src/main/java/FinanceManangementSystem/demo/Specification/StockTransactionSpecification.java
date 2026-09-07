package FinanceManangementSystem.demo.Specification;

import FinanceManangementSystem.demo.Enums.StockTransactionType;
import FinanceManangementSystem.demo.Model.StockTransaction;
import FinanceManangementSystem.demo.Model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StockTransactionSpecification {

    public static Specification<StockTransaction> filter(
            User user,
            UUID stockPublicId,
            StockTransactionType type,
            String referenceNumber,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (user != null) {
                predicates.add(cb.equal(root.get("user"), user));
            }

            if (stockPublicId != null) {
                predicates.add(cb.equal(root.get("stock").get("publicId"), stockPublicId));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("transactionType"), type));
            }

            if (referenceNumber != null && !referenceNumber.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("referenceNumber")), "%" + referenceNumber.trim().toLowerCase() + "%"));
            }

            if (fromDate != null) {
                LocalDateTime start = fromDate.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), start));
            }

            if (toDate != null) {
                LocalDateTime end = toDate.atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), end));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
