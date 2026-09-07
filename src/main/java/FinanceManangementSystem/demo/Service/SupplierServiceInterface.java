package FinanceManangementSystem.demo.Service;

import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestSupplierDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponsePartyStatementDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseSupplierDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SupplierServiceInterface {

    ResponseSupplierDTO addSupplier(
            RequestSupplierDTO dto
    );

    ResponseSupplierDTO getSupplierByPublicId(
            UUID publicId
    );

    List<ResponseSupplierDTO> getAllSuppliers();

    List<ResponseSupplierDTO> getAllActiveSuppliers();

    ResponseSupplierDTO updateSupplier(
            UUID publicId,
            RequestSupplierDTO dto
    );

    void deactivateSupplier(
            UUID publicId
    );

    void reactivateSupplier(
            UUID publicId
    );

    ResponsePartyStatementDTO getSupplierStatement(
            UUID publicId,
            LocalDate fromDate,
            LocalDate toDate
    );
}