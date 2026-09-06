package FinanceManangementSystem.demo.Payloads.ResponseDTO;

import FinanceManangementSystem.demo.Enums.WeightUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseStockReportDTO {

    private UUID publicId;
    private String rawMaterial;
    private WeightUnit unit;
    private BigDecimal currentQuantity;
    private BigDecimal minimumStockLevel;

    private BigDecimal valuationRate;
    private BigDecimal totalValuation;
    private String stockStatus; // "HEALTHY", "LOW_STOCK", "OUT_OF_STOCK"
}
