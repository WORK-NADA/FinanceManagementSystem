package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecentActivityDTO {

    private String activityType; // SALE_INVOICE, PURCHASE_BILL, EXPENSE
    private String documentNumber;
    private String partyName;
    private BigDecimal amount;
    private LocalDate date;
    private String status;

    public RecentActivityDTO() {}

    public RecentActivityDTO(String activityType, String documentNumber, String partyName, BigDecimal amount, LocalDate date, String status) {
        this.activityType = activityType;
        this.documentNumber = documentNumber;
        this.partyName = partyName;
        this.amount = amount;
        this.date = date;
        this.status = status;
    }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
