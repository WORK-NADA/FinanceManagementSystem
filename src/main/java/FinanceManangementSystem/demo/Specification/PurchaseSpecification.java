package FinanceManangementSystem.demo.Specification;

import FinanceManangementSystem.demo.Enums.PaymentStatus;
import FinanceManangementSystem.demo.Model.Purchase;
import FinanceManangementSystem.demo.Model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PurchaseSpecification {

    public static Specification<Purchase> filter(
            User user,
            UUID supplierPublicId,
            PaymentStatus paymentStatus,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (user != null) {
                predicates.add(cb.equal(root.get("user"), user));
            }

            if (supplierPublicId != null) {
                predicates.add(cb.equal(root.get("supplier").get("publicId"), supplierPublicId));
            }

            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("purchaseDate"), fromDate));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("purchaseDate"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
