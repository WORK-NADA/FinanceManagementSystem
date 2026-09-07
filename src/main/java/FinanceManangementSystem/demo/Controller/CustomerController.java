package FinanceManangementSystem.demo.Controller;

import FinanceManangementSystem.demo.APIResponse.APIResponse;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestCustomerDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseCustomerDTO;
import FinanceManangementSystem.demo.Service.CustomerServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponsePartyStatementDTO;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerServiceInterface customerService;


    // =========================================================
    // ADD CUSTOMER
    // =========================================================

    @PostMapping("add")
    public ResponseEntity<APIResponse<ResponseCustomerDTO>> addCustomer(
            @Valid @RequestBody RequestCustomerDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in addCustomer..."
        );


        ResponseCustomerDTO response =
                customerService.addCustomer(dto);


        log.info(
                "CONTROLLER - customer added successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new APIResponse<>(
                                "Customer added successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET CUSTOMER BY PUBLIC ID
    // =========================================================

    @GetMapping("/{publicId}")
    public ResponseEntity<APIResponse<ResponseCustomerDTO>>
    getCustomerByPublicId(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in getCustomerByPublicId..."
        );


        ResponseCustomerDTO response =
                customerService.getCustomerByPublicId(
                        publicId
                );


        log.info(
                "CONTROLLER - customer fetched successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Customer fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET ALL CUSTOMERS
    // =========================================================

    @GetMapping("all")
    public ResponseEntity<APIResponse<List<ResponseCustomerDTO>>>
    getAllCustomers() {

        log.info(
                "CONTROLLER - request came in getAllCustomers..."
        );


        List<ResponseCustomerDTO> response =
                customerService.getAllCustomers();


        log.info(
                "CONTROLLER - customers fetched successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Customers fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET ALL ACTIVE CUSTOMERS
    // =========================================================

    @GetMapping("active")
    public ResponseEntity<APIResponse<List<ResponseCustomerDTO>>>
    getAllActiveCustomers() {

        log.info(
                "CONTROLLER - request came in getAllActiveCustomers..."
        );


        List<ResponseCustomerDTO> response =
                customerService.getAllActiveCustomers();


        log.info(
                "CONTROLLER - active customers fetched successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Active customers fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // UPDATE CUSTOMER
    // =========================================================

    @PutMapping("/{publicId}")
    public ResponseEntity<APIResponse<ResponseCustomerDTO>>
    updateCustomer(
            @PathVariable UUID publicId,
            @Valid @RequestBody RequestCustomerDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in updateCustomer..."
        );


        ResponseCustomerDTO response =
                customerService.updateCustomer(
                        publicId,
                        dto
                );


        log.info(
                "CONTROLLER - customer updated successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Customer updated successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // DEACTIVATE CUSTOMER
    // =========================================================

    @PatchMapping("/{publicId}/deactivate")
    public ResponseEntity<APIResponse<Void>>
    deactivateCustomer(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in deactivateCustomer..."
        );


        customerService.deactivateCustomer(
                publicId
        );


        log.info(
                "CONTROLLER - customer deactivated successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Customer deactivated successfully...",
                                null
                        )
                );
    }


    // =========================================================
    // REACTIVATE CUSTOMER
    // =========================================================

    @PatchMapping("/{publicId}/reactivate")
    public ResponseEntity<APIResponse<Void>>
    reactivateCustomer(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in reactivateCustomer..."
        );


        customerService.reactivateCustomer(
                publicId
        );


        log.info(
                "CONTROLLER - customer reactivated successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Customer reactivated successfully...",
                                null
                        )
                );
    }


    // =========================================================
    // GET CUSTOMER STATEMENT (LEDGER)
    // =========================================================

    @GetMapping("/{publicId}/statement")
    public ResponseEntity<APIResponse<ResponsePartyStatementDTO>> getCustomerStatement(
            @PathVariable UUID publicId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {

        log.info(
                "CONTROLLER - request came in getCustomerStatement for publicId={}, fromDate={}, toDate={}",
                publicId, fromDate, toDate
        );

        ResponsePartyStatementDTO response =
                customerService.getCustomerStatement(publicId, fromDate, toDate);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Customer statement fetched successfully...",
                                response
                        )
                );
    }
}