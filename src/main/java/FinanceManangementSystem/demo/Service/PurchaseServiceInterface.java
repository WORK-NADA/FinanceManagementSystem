package FinanceManangementSystem.demo.Service;

import FinanceManangementSystem.demo.Enums.PaymentStatus;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestPurchaseDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponsePurchaseDTO;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface PurchaseServiceInterface {

    // =========================================================
    // CREATE PURCHASE
    // =========================================================

    ResponsePurchaseDTO addPurchase(
            RequestPurchaseDTO dto
    );


    // =========================================================
    // GET PURCHASE BY PUBLIC ID
    // =========================================================

    ResponsePurchaseDTO getPurchaseByPublicId(
            UUID publicId
    );


    // =========================================================
    // GET ALL PURCHASES
    // =========================================================

    default Page<ResponsePurchaseDTO> getAllPurchases(Pageable pageable) {
        return getAllPurchases(null, null, null, null, pageable);
    }

    Page<ResponsePurchaseDTO> getAllPurchases(
            UUID supplierPublicId,
            PaymentStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );


    // =========================================================
    // GET PURCHASES BY SUPPLIER
    // =========================================================

    List<ResponsePurchaseDTO> getPurchasesBySupplier(
            UUID supplierPublicId
    );


    // =========================================================
    // GET PURCHASES BY DATE RANGE
    // =========================================================

    List<ResponsePurchaseDTO> getPurchasesByDateRange(
            LocalDate fromDate,
            LocalDate toDate
    );


    // =========================================================
    // GET PURCHASES BY STATUS
    // =========================================================

    List<ResponsePurchaseDTO> getPurchasesByStatus(
            String status
    );


    // =========================================================
    // UPDATE PURCHASE
    // =========================================================

    ResponsePurchaseDTO updatePurchase(
            UUID publicId,
            RequestPurchaseDTO dto
    );


    // =========================================================
    // DELETE PURCHASE
    // =========================================================

    void deletePurchase(
            UUID publicId
    );
}