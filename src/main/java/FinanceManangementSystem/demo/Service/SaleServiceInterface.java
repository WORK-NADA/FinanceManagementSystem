package FinanceManangementSystem.demo.Service;

import FinanceManangementSystem.demo.Enums.PaymentStatus;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestSaleDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseSaleDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SaleServiceInterface {

    ResponseSaleDTO addSale(
            RequestSaleDTO dto
    );

    ResponseSaleDTO getSaleByPublicId(
            UUID publicId
    );

    default Page<ResponseSaleDTO> getAllSales(Pageable pageable) {
        return getAllSales(null, null, null, null, pageable);
    }

    Page<ResponseSaleDTO> getAllSales(
            UUID customerPublicId,
            PaymentStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    // =========================================================
    // GET SALES BY CUSTOMER
    // =========================================================

    List<ResponseSaleDTO> getSalesByCustomer(
            UUID customerPublicId
    );

    ResponseSaleDTO updateSale(
            UUID publicId,
            RequestSaleDTO dto
    );

    void deleteSale(
            UUID publicId
    );
}