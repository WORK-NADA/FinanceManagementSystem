package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import FinanceManangementSystem.demo.Enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ResponseSaleReportDTO {

    private UUID publicId;
    private LocalDate saleDate;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private String customerName;
    private String saleNumber;
    private String customerInvoiceNumber;
    private String rawMaterial;
    private BigDecimal weight;
    private String unit;
    private BigDecimal ratePerUnit;

    public ResponseSaleReportDTO() {}

    public UUID getPublicId() { return publicId; }
    public void setPublicId(UUID publicId) { this.publicId = publicId; }
    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getSaleNumber() { return saleNumber; }
    public void setSaleNumber(String saleNumber) { this.saleNumber = saleNumber; }
    public String getCustomerInvoiceNumber() { return customerInvoiceNumber; }
    public void setCustomerInvoiceNumber(String customerInvoiceNumber) { this.customerInvoiceNumber = customerInvoiceNumber; }
    public String getRawMaterial() { return rawMaterial; }
    public void setRawMaterial(String rawMaterial) { this.rawMaterial = rawMaterial; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getRatePerUnit() { return ratePerUnit; }
    public void setRatePerUnit(BigDecimal ratePerUnit) { this.ratePerUnit = ratePerUnit; }
}
