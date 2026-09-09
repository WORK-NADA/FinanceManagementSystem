package FinanceManangementSystem.demo.Service;

import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestInvestmentDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseInvestmentDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InvestmentServiceInterface {

    ResponseInvestmentDTO addInvestment(RequestInvestmentDTO dto);

    ResponseInvestmentDTO updateInvestment(UUID publicId, RequestInvestmentDTO dto);

    void deleteInvestment(UUID publicId);

    ResponseInvestmentDTO getInvestmentByPublicId(UUID publicId);

    List<ResponseInvestmentDTO> getAllInvestments();

    BigDecimal getTotalInvestments();
}
