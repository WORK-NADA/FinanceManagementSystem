package FinanceManangementSystem.demo.Controller;

import FinanceManangementSystem.demo.APIResponse.APIResponse;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestSupplierDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseSupplierDTO;
import FinanceManangementSystem.demo.Service.SupplierServiceInterface;
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
@RequestMapping("supplier")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierServiceInterface supplierService;


    // ==================================================
    // ADD SUPPLIER
    // ==================================================

    @PostMapping("add")
    public ResponseEntity<APIResponse<ResponseSupplierDTO>> addSupplier(
            @Valid @RequestBody RequestSupplierDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in addSupplier..."
        );


        log.info(
                "CONTROLLER - calling supplier service..."
        );

        ResponseSupplierDTO response =
                supplierService.addSupplier(dto);


        log.info(
                "CONTROLLER - supplier added successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new APIResponse<>(
                                "Supplier added successfully...",
                                response
                        )
                );
    }


    // ==================================================
    // GET SUPPLIER
    // ==================================================

    @GetMapping("/{publicId}")
    public ResponseEntity<APIResponse<ResponseSupplierDTO>>
    getSupplierByPublicId(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in getSupplierByPublicId..."
        );


        log.info(
                "CONTROLLER - calling supplier service..."
        );

        ResponseSupplierDTO response =
                supplierService.getSupplierByPublicId(
                        publicId
                );


        log.info(
                "CONTROLLER - supplier fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Supplier fetched successfully...",
                                response
                        )
                );
    }


    // ==================================================
    // GET ALL SUPPLIERS
    // ==================================================

    @GetMapping("all")
    public ResponseEntity<APIResponse<List<ResponseSupplierDTO>>>
    getAllSuppliers() {

        log.info(
                "CONTROLLER - request came in getAllSuppliers..."
        );


        log.info(
                "CONTROLLER - calling supplier service..."
        );

        List<ResponseSupplierDTO> response =
                supplierService.getAllSuppliers();


        log.info(
                "CONTROLLER - suppliers fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Suppliers fetched successfully...",
                                response
                        )
                );
    }


    // ==================================================
    // GET ALL ACTIVE SUPPLIERS
    // ==================================================

    @GetMapping("active")
    public ResponseEntity<APIResponse<List<ResponseSupplierDTO>>>
    getAllActiveSuppliers() {

        log.info(
                "CONTROLLER - request came in getAllActiveSuppliers..."
        );


        log.info(
                "CONTROLLER - calling supplier service..."
        );

        List<ResponseSupplierDTO> response =
                supplierService.getAllActiveSuppliers();


        log.info(
                "CONTROLLER - active suppliers fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Active suppliers fetched successfully...",
                                response
                        )
                );
    }

    // ==================================================
    // UPDATE SUPPLIER
    // ==================================================

    @PutMapping("/{publicId}")
    public ResponseEntity<APIResponse<ResponseSupplierDTO>>
    updateSupplier(
            @PathVariable UUID publicId,
            @Valid @RequestBody RequestSupplierDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in updateSupplier..."
        );


        log.info(
                "CONTROLLER - calling supplier service..."
        );

        ResponseSupplierDTO response =
                supplierService.updateSupplier(
                        publicId,
                        dto
                );


        log.info(
                "CONTROLLER - supplier updated successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Supplier updated successfully...",
                                response
                        )
                );
    }


    // ==================================================
    // DEACTIVATE SUPPLIER
    // ==================================================

    @PatchMapping("/{publicId}/deactivate")
    public ResponseEntity<APIResponse<Void>>
    deactivateSupplier(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in deactivateSupplier..."
        );


        log.info(
                "CONTROLLER - calling supplier service..."
        );

        supplierService.deactivateSupplier(publicId);


        log.info(
                "CONTROLLER - supplier deactivated successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Supplier deactivated successfully...",
                                null
                        )
                );
    }


    // ==================================================
    // ACTIVATE SUPPLIER
    // ==================================================

    @PatchMapping("/{publicId}/reactivate")
    public ResponseEntity<APIResponse<Void>>
    reactivateSupplier(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in reactivateSupplier..."
        );


        log.info(
                "CONTROLLER - calling supplier service..."
        );

        supplierService.reactivateSupplier(publicId);


        log.info(
                "CONTROLLER - supplier activated successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Supplier activated successfully...",
                                null
                        )
                );
    }


    // ==================================================
    // GET SUPPLIER STATEMENT (LEDGER)
    // ==================================================

    @GetMapping("/{publicId}/statement")
    public ResponseEntity<APIResponse<ResponsePartyStatementDTO>> getSupplierStatement(
            @PathVariable UUID publicId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {

        log.info(
                "CONTROLLER - request came in getSupplierStatement for publicId={}, fromDate={}, toDate={}",
                publicId, fromDate, toDate
        );

        ResponsePartyStatementDTO response =
                supplierService.getSupplierStatement(publicId, fromDate, toDate);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Supplier statement fetched successfully...",
                                response
                        )
                );
    }
}