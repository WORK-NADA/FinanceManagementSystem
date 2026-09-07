package FinanceManangementSystem.demo.Service;

import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestCustomerDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseCustomerDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponsePartyStatementDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CustomerServiceInterface {

    // =========================================================
    // ADD CUSTOMER
    // =========================================================

    ResponseCustomerDTO addCustomer(
            RequestCustomerDTO dto
    );


    // =========================================================
    // GET CUSTOMER BY PUBLIC ID
    // =========================================================

    ResponseCustomerDTO getCustomerByPublicId(
            UUID publicId
    );


    // =========================================================
    // GET ALL CUSTOMERS
    // =========================================================

    List<ResponseCustomerDTO> getAllCustomers();


    // =========================================================
    // GET ALL ACTIVE CUSTOMERS
    // =========================================================

    List<ResponseCustomerDTO> getAllActiveCustomers();


    // =========================================================
    // UPDATE CUSTOMER
    // =========================================================

    ResponseCustomerDTO updateCustomer(
            UUID publicId,
            RequestCustomerDTO dto
    );


    // =========================================================
    // DEACTIVATE CUSTOMER
    // =========================================================

    void deactivateCustomer(
            UUID publicId
    );


    // =========================================================
    // REACTIVATE CUSTOMER
    // =========================================================

    void reactivateCustomer(
            UUID publicId
    );


    // =========================================================
    // GET CUSTOMER STATEMENT (LEDGER)
    // =========================================================

    ResponsePartyStatementDTO getCustomerStatement(
            UUID publicId,
            LocalDate fromDate,
            LocalDate toDate
    );
}