package FinanceManangementSystem.demo.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ExpenseCategory {
    RENT,
    ELECTRICITY,
    TRANSPORT,
    SALARY,
    MAINTENANCE,
    OFFICE_SUPPLIES,
    OTHER;

    @JsonCreator
    public static ExpenseCategory fromString(String value) {
        if (value == null) return null;
        String v = value.trim().toUpperCase();
        if ("MISCELLANEOUS".equals(v)) {
            return OTHER;
        }
        return ExpenseCategory.valueOf(v);
    }
}
