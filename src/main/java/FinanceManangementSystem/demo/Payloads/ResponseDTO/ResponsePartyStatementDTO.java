package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponsePartyStatementDTO {

    private UUID partyPublicId;
    private String partyName;
    private String mobileNumber;
    private String email;
    private String gstNumber;
    private String city;
    private String address;

    private BigDecimal openingBalance;
    private BigDecimal totalBilled;
    private BigDecimal totalPaid;
    private BigDecimal outstandingBalance;

    private List<LedgerEntryDTO> entries;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LedgerEntryDTO {
        private LocalDate date;
        private String entryType;        // "INVOICE" | "PAYMENT" (customer), "BILL" | "PAYMENT" (supplier)
        private String documentNumber;   // "INV-2026-0001", "SPY-2026-0002", "PUR-2026-0001", "PPY-2026-0001"
        private String referenceNumber;  // Party invoice # or payment ref
        private String description;      // Item summary or payment description
        private BigDecimal debit;        // Customer invoice / Supplier payment
        private BigDecimal credit;       // Customer payment / Supplier bill
        private BigDecimal runningBalance;
        private String paymentMode;      // CASH, UPI, BANK_TRANSFER, CHEQUE
        private String remarks;
    }
}
