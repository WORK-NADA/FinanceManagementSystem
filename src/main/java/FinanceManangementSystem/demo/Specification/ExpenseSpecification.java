package FinanceManangementSystem.demo.Specification;

import FinanceManangementSystem.demo.Enums.ExpenseCategory;
import FinanceManangementSystem.demo.Model.Expense;
import FinanceManangementSystem.demo.Model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExpenseSpecification {

    public static Specification<Expense> filter(
            User user,
            ExpenseCategory category,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (user != null) {
                predicates.add(cb.equal(root.get("user"), user));
            }

            predicates.add(cb.equal(root.get("isActive"), true));

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), fromDate));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
