package FinanceManangementSystem.demo.Service;

import FinanceManangementSystem.demo.Payloads.ResponseDTO.DashboardSummaryDTO;
import java.math.BigDecimal;

public interface DashboardServiceInterface {

    DashboardSummaryDTO getDashboardSummary();

    DashboardSummaryDTO updateOpeningBalance(BigDecimal openingBalance);

}
